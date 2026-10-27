package rd.alzada.payments;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.Texts;
import rd.alzada.contracts.Contract;
import rd.alzada.contracts.ContractRepository;
import rd.alzada.contracts.ContractPartyRepository;
import rd.alzada.customers.Customer;
import rd.alzada.customers.CustomerRepository;
import rd.alzada.documents.DocumentFile;
import rd.alzada.documents.DocumentService;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.notifications.NotificationService;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.tenants.CompanyRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinanceService {

  private static final Logger log = LoggerFactory.getLogger(FinanceService.class);
  private static final ZoneId SANTO_DOMINGO = ZoneId.of("America/Santo_Domingo");

  private final PaymentPlanRepository plans;
  private final InstallmentRepository installments;
  private final PaymentRepository payments;
  private final PaymentAllocationRepository allocations;
  private final ContractRepository contracts;
  private final ContractPartyRepository parties;
  private final CustomerRepository customers;
  private final ProjectRepository projects;
  private final UnitRepository units;
  private final DocumentService documents;
  private final NotificationService notifications;
  private final AuditService audit;

  @PersistenceContext
  private EntityManager entityManager;

  public FinanceService(
      PaymentPlanRepository plans,
      InstallmentRepository installments,
      PaymentRepository payments,
      PaymentAllocationRepository allocations,
      ContractRepository contracts,
      ContractPartyRepository parties,
      CustomerRepository customers,
      ProjectRepository projects,
      UnitRepository units,
      @Lazy DocumentService documents,
      NotificationService notifications,
      AuditService audit) {
    this.plans = plans;
    this.installments = installments;
    this.payments = payments;
    this.allocations = allocations;
    this.contracts = contracts;
    this.parties = parties;
    this.customers = customers;
    this.projects = projects;
    this.units = units;
    this.documents = documents;
    this.notifications = notifications;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public PlanView plan(UUID contractId) {
    Contract contract = requireContract(contractId);
    return view(contract);
  }

  @Transactional
  public PlanView savePlan(UUID contractId, PlanCommand command) {
    UUID tenantId = SecuritySupport.tenantId();
    Contract contract = contracts.lock(contractId, tenantId).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() != Contract.Status.DRAFT && contract.getStatus() != Contract.Status.ACTIVE) {
      throw ApiException.conflict("El contrato no admite cambios en el plan");
    }
    if (payments.existsByContractIdAndStatus(contractId, Payment.Status.VALIDATED)
        || payments.existsByContractIdAndStatus(contractId, Payment.Status.REPORTED)) {
      throw ApiException.conflict("No se puede regenerar el plan porque ya hay pagos registrados");
    }
    List<DraftInstallment> schedule = schedule(command, contract);
    PaymentPlan plan = plans.findByContractIdAndTenantId(contractId, tenantId).orElseGet(PaymentPlan::new);
    if (plan.getId() != null) {
      installments.deleteByPlanIdAndTenantId(plan.getId(), tenantId);
      entityManager.flush();
    }
    plan.setTenantId(tenantId);
    plan.setContractId(contractId);
    plan.setTotalPrice(scale(command.totalPrice()));
    plan.setDownPayment(scale(command.downPayment()));
    plan.setInstallmentCount(command.installmentCount());
    plan.setFrequency(command.frequency());
    plan.setCurrency(contract.getCurrency());
    plan.setDeliveryAmount(command.deliveryAmount() == null ? null : scale(command.deliveryAmount()));
    plan.setConditions(Texts.blankToNull(command.conditions()));
    plans.save(plan);
    int sequence = 1;
    for (DraftInstallment draft : schedule) {
      Installment installment = new Installment();
      installment.setTenantId(tenantId);
      installment.setPlanId(plan.getId());
      installment.setContractId(contractId);
      installment.setSequenceNumber(sequence++);
      installment.setKind(draft.kind());
      installment.setLabel(draft.label());
      installment.setDueDate(draft.dueDate());
      installment.setAmount(draft.amount());
      installment.setPaidAmount(BigDecimal.ZERO);
      installment.setStatus(draft.dueDate().isBefore(LocalDate.now(SANTO_DOMINGO)) ? Installment.Status.OVERDUE : Installment.Status.PENDING);
      installments.save(installment);
    }
    audit.record("PAYMENT_PLAN_SAVED", "PAYMENT_PLAN", plan.getId(), "Configuró el plan de pagos", Map.of("contractId", contractId.toString()));
    return view(contract);
  }

  @Transactional(readOnly = true)
  public PageResponse<Payment> search(Payment.Status status, UUID contractId, int page, int size) {
    return PageResponse.of(payments.search(SecuritySupport.tenantId(), status, contractId, Pages.desc(page, size)));
  }

  @Transactional
  public Payment register(UUID contractId, BigDecimal amount, String reference, String notes, List<AllocationCommand> requested, MultipartFile receipt) {
    return receive(contractId, amount, reference, notes, requested, receipt, true);
  }

  @Transactional
  public Payment reportByBuyer(UUID contractId, BigDecimal amount, String reference, String notes, MultipartFile receipt) {
    if (receipt == null || receipt.isEmpty()) {
      throw ApiException.badRequest("Adjunte el comprobante de pago");
    }
    return receive(contractId, amount, reference, notes, List.of(), receipt, false);
  }

  @Transactional
  public Payment validate(UUID id, List<AllocationCommand> requested) {
    Payment payment = payments.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El pago no existe"));
    if (payment.getStatus() != Payment.Status.REPORTED) {
      throw ApiException.conflict("Solo se puede confirmar un pago reportado");
    }
    applyAllocations(payment, requested);
    payment.setStatus(Payment.Status.VALIDATED);
    payment.setValidatedBy(SecuritySupport.currentUserId());
    payment.setValidatedAt(Instant.now());
    notifyBuyer(payment, "RECEIPT_CONFIRMED", "Su pago fue confirmado", "El pago quedó aplicado al plan.", null);
    audit.record("PAYMENT_VALIDATED", "PAYMENT", payment.getId(), "Confirmó un pago", Map.of("amount", payment.getAmount().toPlainString()));
    return payment;
  }

  @Transactional
  public Payment reject(UUID id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw ApiException.badRequest("Indique el motivo del rechazo");
    }
    Payment payment = payments.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El pago no existe"));
    if (payment.getStatus() != Payment.Status.REPORTED) {
      throw ApiException.conflict("Solo se puede rechazar un pago reportado");
    }
    payment.setStatus(Payment.Status.REJECTED);
    payment.setRejectionReason(reason.trim());
    payment.setValidatedBy(SecuritySupport.currentUserId());
    payment.setValidatedAt(Instant.now());
    notifyBuyer(payment, "RECEIPT_REJECTED", "Su comprobante no fue aceptado", reason.trim(), reason.trim());
    audit.record("PAYMENT_REJECTED", "PAYMENT", payment.getId(), "Rechazó un comprobante", Map.of());
    return payment;
  }

  @Transactional
  public Payment reverse(UUID id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw ApiException.badRequest("Indique el motivo de la reversión");
    }
    Payment payment = payments.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El pago no existe"));
    if (payment.getStatus() != Payment.Status.VALIDATED) {
      throw ApiException.conflict("Solo se puede reversar un pago confirmado");
    }
    allocations.findByPaymentIdAndTenantId(payment.getId(), payment.getTenantId()).forEach(allocation -> allocation.setVoided(true));
    entityManager.flush();
    installments.lockByContract(payment.getContractId(), payment.getTenantId()).forEach(this::refresh);
    payment.setStatus(Payment.Status.REVERSED);
    payment.setReversedBy(SecuritySupport.currentUserId());
    payment.setReversedAt(Instant.now());
    payment.setReversalReason(reason.trim());
    audit.record("PAYMENT_REVERSED", "PAYMENT", payment.getId(), "Reversó un pago confirmado", Map.of("reason", reason.trim()));
    return payment;
  }

  @Transactional
  public List<Installment> newlyOverdue() {
    if (!lock(8421003)) {
      return List.of();
    }
    return installments.findNewlyOverdue(LocalDate.now(SANTO_DOMINGO));
  }

  @Transactional
  public List<Installment> upcoming() {
    if (!lock(8421004)) {
      return List.of();
    }
    LocalDate today = LocalDate.now(SANTO_DOMINGO);
    return installments.findUpcoming(today, today.plusDays(3));
  }

  @Transactional
  public void markOverdue(UUID id, UUID tenantId) {
    Installment installment = installments.findById(id).filter(item -> item.getTenantId().equals(tenantId)).orElse(null);
    if (installment == null || installment.getOverdueNoticeSentAt() != null) {
      return;
    }
    refresh(installment);
    if (installment.getStatus() != Installment.Status.OVERDUE) {
      return;
    }
    installment.setOverdueNoticeSentAt(Instant.now());
    notifyInstallment(installment, "INSTALLMENT_OVERDUE", "Tiene una cuota vencida");
    notifications.notifyRole(CompanyRole.FINANCE, "INSTALLMENT_OVERDUE", "Cuota vencida",
        "La cuota " + installment.getLabel() + " está vencida.",
        vars(installment));
  }

  @Transactional
  public void remind(UUID id, UUID tenantId) {
    Installment installment = installments.findById(id).filter(item -> item.getTenantId().equals(tenantId)).orElse(null);
    if (installment == null || installment.getUpcomingNoticeSentAt() != null) {
      return;
    }
    if (installment.getStatus() == Installment.Status.PAID || installment.getStatus() == Installment.Status.CANCELLED) {
      return;
    }
    installment.setUpcomingNoticeSentAt(Instant.now());
    notifyInstallment(installment, "INSTALLMENT_UPCOMING", "Tiene una cuota próxima a vencer");
  }

  private Payment receive(UUID contractId, BigDecimal amount, String reference, String notes, List<AllocationCommand> requested, MultipartFile receipt, boolean validateNow) {
    if (amount == null || amount.signum() <= 0) {
      throw ApiException.badRequest("El monto debe ser mayor que cero");
    }
    UUID tenantId = SecuritySupport.tenantId();
    Contract contract = contracts.lock(contractId, tenantId).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() != Contract.Status.ACTIVE) {
      throw ApiException.conflict("Solo se registran pagos de un contrato activo");
    }
    Payment payment = new Payment();
    payment.setTenantId(tenantId);
    payment.setContractId(contractId);
    payment.setAmount(scale(amount));
    payment.setCurrency(contract.getCurrency());
    payment.setReferenceNumber(Texts.blankToNull(reference));
    payment.setNotes(Texts.blankToNull(notes));
    payment.setReportedBy(SecuritySupport.currentUserId());
    payment.setStatus(validateNow ? Payment.Status.VALIDATED : Payment.Status.REPORTED);
    if (validateNow) {
      payment.setValidatedBy(SecuritySupport.currentUserId());
      payment.setValidatedAt(Instant.now());
    }
    payments.save(payment);
    if (receipt != null && !receipt.isEmpty()) {
      DocumentFile document = documents.store(DocumentFile.OwnerType.PAYMENT, payment.getId(), DocumentFile.Visibility.BUYER_VISIBLE, receipt);
      payment.setReceiptDocumentId(document.getId());
    }
    if (validateNow) {
      applyAllocations(payment, requested);
      audit.record("PAYMENT_REGISTERED", "PAYMENT", payment.getId(), "Registró un pago confirmado", Map.of("amount", payment.getAmount().toPlainString()));
    } else {
      notifications.notifyRole(CompanyRole.FINANCE, "RECEIPT_REPORTED", "Comprobante por validar",
          "Un comprador reportó un pago de " + payment.getAmount().toPlainString() + " " + payment.getCurrency() + ".",
          Map.of("amount", payment.getAmount().toPlainString(), "recipientName", "finanzas"));
    }
    return payment;
  }

  private void applyAllocations(Payment payment, List<AllocationCommand> requested) {
    List<Installment> rows = installments.lockByContract(payment.getContractId(), payment.getTenantId());
    BigDecimal left = payment.getAmount();
    if (requested != null && !requested.isEmpty()) {
      BigDecimal sum = requested.stream().map(item -> scale(item.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
      if (sum.compareTo(payment.getAmount()) != 0) {
        throw ApiException.badRequest("La distribución debe coincidir con el monto del pago");
      }
      for (AllocationCommand command : requested) {
        Installment installment = rows.stream().filter(item -> item.getId().equals(command.installmentId())).findFirst()
            .orElseThrow(() -> ApiException.badRequest("La cuota no pertenece al contrato"));
        BigDecimal open = installment.getAmount().subtract(installment.getPaidAmount());
        if (scale(command.amount()).compareTo(open) > 0) {
          throw ApiException.badRequest("El abono supera el saldo de la cuota " + installment.getLabel());
        }
        saveAllocation(payment, installment, scale(command.amount()));
      }
    } else {
      for (Installment installment : rows) {
        if (left.signum() == 0) {
          break;
        }
        if (installment.getStatus() == Installment.Status.PAID || installment.getStatus() == Installment.Status.CANCELLED) {
          continue;
        }
        BigDecimal open = installment.getAmount().subtract(installment.getPaidAmount());
        if (open.signum() <= 0) {
          continue;
        }
        BigDecimal take = open.min(left);
        saveAllocation(payment, installment, take);
        left = left.subtract(take);
      }
      if (left.signum() > 0) {
        throw ApiException.badRequest("El pago supera el saldo pendiente del plan");
      }
    }
    rows.forEach(this::refresh);
  }

  private void saveAllocation(Payment payment, Installment installment, BigDecimal amount) {
    if (amount.signum() <= 0) {
      return;
    }
    PaymentAllocation allocation = new PaymentAllocation();
    allocation.setTenantId(payment.getTenantId());
    allocation.setPaymentId(payment.getId());
    allocation.setInstallmentId(installment.getId());
    allocation.setAmount(amount);
    allocation.setVoided(false);
    allocations.save(allocation);
    installment.setPaidAmount(installment.getPaidAmount().add(amount));
  }

  private void refresh(Installment installment) {
    BigDecimal paid = allocations.findByInstallmentIdAndVoidedFalse(installment.getId()).stream()
        .map(PaymentAllocation::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (paid.compareTo(installment.getAmount()) > 0) {
      paid = installment.getAmount();
    }
    installment.setPaidAmount(paid);
    if (paid.compareTo(installment.getAmount()) >= 0) {
      installment.setStatus(Installment.Status.PAID);
    } else if (installment.getDueDate().isBefore(LocalDate.now(SANTO_DOMINGO))) {
      installment.setStatus(Installment.Status.OVERDUE);
    } else if (paid.signum() > 0) {
      installment.setStatus(Installment.Status.PARTIAL);
    } else {
      installment.setStatus(Installment.Status.PENDING);
    }
  }

  private List<DraftInstallment> schedule(PlanCommand command, Contract contract) {
    if (command.totalPrice() == null || command.totalPrice().signum() <= 0) {
      throw ApiException.badRequest("El precio total debe ser mayor que cero");
    }
    if (command.totalPrice().compareTo(contract.getAgreedPrice()) != 0) {
      throw ApiException.badRequest("El total del plan debe ser igual al precio acordado del contrato");
    }
    BigDecimal down = scale(command.downPayment() == null ? BigDecimal.ZERO : command.downPayment());
    BigDecimal delivery = scale(command.deliveryAmount() == null ? BigDecimal.ZERO : command.deliveryAmount());
    List<PlanCommand.Extra> extras = command.extraordinary() == null ? List.of() : command.extraordinary();
    BigDecimal extraSum = extras.stream().map(extra -> scale(extra.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (down.add(delivery).add(extraSum).compareTo(scale(command.totalPrice())) > 0) {
      throw ApiException.badRequest("La inicial, las cuotas extraordinarias y el pago contra entrega superan el precio total");
    }
    BigDecimal regular = scale(command.totalPrice()).subtract(down).subtract(delivery).subtract(extraSum);
    if (command.installmentCount() < 0 || command.installmentCount() > 600) {
      throw ApiException.badRequest("La cantidad de cuotas no es válida");
    }
    if (regular.signum() > 0 && command.installmentCount() == 0) {
      throw ApiException.badRequest("Indique la cantidad de cuotas para el saldo regular");
    }
    if (regular.signum() == 0 && command.installmentCount() > 0) {
      throw ApiException.badRequest("No queda saldo para cuotas regulares");
    }
    List<DraftInstallment> drafts = new ArrayList<>();
    if (down.signum() > 0) {
      if (command.downPaymentDueDate() == null) {
        throw ApiException.badRequest("Indique la fecha de la inicial");
      }
      drafts.add(new DraftInstallment(Installment.Kind.DOWN_PAYMENT, "Inicial", command.downPaymentDueDate(), down));
    }
    if (command.installmentCount() > 0) {
      if (command.firstDueDate() == null || command.frequency() == null) {
        throw ApiException.badRequest("Indique la frecuencia y la primera fecha de cuota");
      }
      BigDecimal each = regular.divide(BigDecimal.valueOf(command.installmentCount()), 2, RoundingMode.DOWN);
      if (regular.signum() > 0 && each.signum() == 0) {
        throw ApiException.badRequest("El saldo no alcanza para esa cantidad de cuotas");
      }
      BigDecimal consumed = each.multiply(BigDecimal.valueOf(command.installmentCount() - 1L));
      LocalDate due = command.firstDueDate();
      for (int index = 1; index <= command.installmentCount(); index++) {
        BigDecimal amount = index == command.installmentCount() ? regular.subtract(consumed) : each;
        if (amount.signum() > 0) {
          drafts.add(new DraftInstallment(Installment.Kind.REGULAR, "Cuota " + index, due, amount));
        }
        due = advance(due, command.frequency());
      }
    }
    for (PlanCommand.Extra extra : extras) {
      if (extra.dueDate() == null || extra.amount() == null || extra.amount().signum() <= 0) {
        throw ApiException.badRequest("Revise las cuotas extraordinarias");
      }
      drafts.add(new DraftInstallment(Installment.Kind.EXTRAORDINARY, extra.label() == null || extra.label().isBlank() ? "Cuota extraordinaria" : extra.label().trim(), extra.dueDate(), scale(extra.amount())));
    }
    if (delivery.signum() > 0) {
      if (command.deliveryDueDate() == null) {
        throw ApiException.badRequest("Indique la fecha del pago contra entrega");
      }
      drafts.add(new DraftInstallment(Installment.Kind.DELIVERY, "Pago contra entrega", command.deliveryDueDate(), delivery));
    }
    if (drafts.isEmpty()) {
      throw ApiException.badRequest("El plan debe incluir al menos un pago");
    }
    drafts.sort(Comparator.comparing(DraftInstallment::dueDate).thenComparing(item -> item.kind().ordinal()));
    BigDecimal total = drafts.stream().map(DraftInstallment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(scale(command.totalPrice())) != 0) {
      throw ApiException.badRequest("La suma de las cuotas no coincide con el precio total");
    }
    return drafts;
  }

  private PlanView view(Contract contract) {
    PaymentPlan plan = plans.findByContractIdAndTenantId(contract.getId(), contract.getTenantId()).orElse(null);
    List<Installment> rows = installments.findByContractIdAndTenantIdOrderBySequenceNumberAsc(contract.getId(), contract.getTenantId());
    List<Payment> paymentRows = payments.findByContractIdAndTenantIdOrderByCreatedAtDesc(contract.getId(), contract.getTenantId());
    List<PaymentAllocation> allocationRows = new ArrayList<>();
    for (Payment payment : paymentRows) {
      allocationRows.addAll(allocations.findByPaymentIdAndTenantId(payment.getId(), contract.getTenantId()));
    }
    BigDecimal paid = paymentRows.stream().filter(payment -> payment.getStatus() == Payment.Status.VALIDATED).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal total = plan == null ? contract.getAgreedPrice() : plan.getTotalPrice();
    return new PlanView(plan, rows, paymentRows, allocationRows, paid, total.subtract(paid));
  }

  private void notifyBuyer(Payment payment, String type, String title, String body, String reason) {
    parties.findByContractIdAndTenantId(payment.getContractId(), payment.getTenantId()).forEach(party -> {
      Customer customer = customers.findByIdAndTenantId(party.getCustomerId(), payment.getTenantId()).orElse(null);
      if (customer != null && customer.getUserId() != null) {
        notifications.notifyUsers(List.of(customer.getUserId()), type, title, body, Map.of(
            "recipientName", customer.getFullName(),
            "amount", payment.getAmount().toPlainString() + " " + payment.getCurrency(),
            "reason", reason == null ? "" : reason,
            "unitCode", unitCode(payment),
            "companyName", "su constructora"));
      }
    });
  }

  private void notifyInstallment(Installment installment, String type, String title) {
    Map<String, String> variables = vars(installment);
    parties.findByContractIdAndTenantId(installment.getContractId(), installment.getTenantId()).forEach(party -> {
      Customer customer = customers.findByIdAndTenantId(party.getCustomerId(), installment.getTenantId()).orElse(null);
      if (customer != null && customer.getUserId() != null) {
        notifications.notifyUsers(List.of(customer.getUserId()), type, title,
            installment.getLabel() + " por " + installment.getAmount().toPlainString() + " vence el " + installment.getDueDate() + ".",
            variables);
      }
    });
  }

  private Map<String, String> vars(Installment installment) {
    BigDecimal balance = installment.getAmount().subtract(installment.getPaidAmount());
    return Map.of(
        "recipientName", "comprador",
        "installmentLabel", installment.getLabel(),
        "amount", installment.getAmount().toPlainString(),
        "balance", balance.toPlainString(),
        "dueDate", installment.getDueDate().toString(),
        "projectName", projectName(installment.getContractId(), installment.getTenantId()),
        "companyName", "su constructora");
  }

  private String unitCode(Payment payment) {
    return contracts.findById(payment.getContractId())
        .flatMap(contract -> units.findById(contract.getUnitId()))
        .map(unit -> unit.getCode())
        .orElse("");
  }

  private String projectName(UUID contractId, UUID tenantId) {
    return contracts.findByIdAndTenantId(contractId, tenantId)
        .flatMap(contract -> projects.findByIdAndTenantId(contract.getProjectId(), tenantId))
        .map(project -> project.getName())
        .orElse("su proyecto");
  }

  private Contract requireContract(UUID id) {
    return contracts.findByIdAndTenantId(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
  }

  private boolean lock(int key) {
    Object result = entityManager.createNativeQuery("select pg_try_advisory_xact_lock(" + key + ")").getSingleResult();
    boolean locked = Boolean.TRUE.equals(result);
    if (!locked) {
      log.debug("Mantenimiento {} en curso", key);
    }
    return locked;
  }

  private static BigDecimal scale(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private static LocalDate advance(LocalDate date, PaymentPlan.Frequency frequency) {
    return switch (frequency) {
      case WEEKLY -> date.plusWeeks(1);
      case BIWEEKLY -> date.plusWeeks(2);
      case MONTHLY -> date.plusMonths(1);
      case QUARTERLY -> date.plusMonths(3);
    };
  }

  public record PlanCommand(
      BigDecimal totalPrice,
      BigDecimal downPayment,
      LocalDate downPaymentDueDate,
      int installmentCount,
      PaymentPlan.Frequency frequency,
      LocalDate firstDueDate,
      BigDecimal deliveryAmount,
      LocalDate deliveryDueDate,
      List<Extra> extraordinary,
      String conditions) {
    public record Extra(String label, LocalDate dueDate, BigDecimal amount) {}
  }

  public record AllocationCommand(UUID installmentId, BigDecimal amount) {}

  public record PlanView(
      PaymentPlan plan,
      List<Installment> installments,
      List<Payment> payments,
      List<PaymentAllocation> allocations,
      BigDecimal paid,
      BigDecimal balance) {}

  private record DraftInstallment(Installment.Kind kind, String label, LocalDate dueDate, BigDecimal amount) {}
}
