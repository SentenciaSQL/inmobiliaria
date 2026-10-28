package rd.alzada.contracts;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.Texts;
import rd.alzada.customers.CustomerRepository;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.payments.InstallmentRepository;
import rd.alzada.payments.Payment;
import rd.alzada.payments.PaymentPlanRepository;
import rd.alzada.payments.PaymentRepository;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.reservations.Reservation;
import rd.alzada.reservations.ReservationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

  private final ContractRepository contracts;
  private final ContractPartyRepository parties;
  private final ProjectRepository projects;
  private final UnitRepository units;
  private final CustomerRepository customers;
  private final ReservationRepository reservations;
  private final PaymentPlanRepository plans;
  private final InstallmentRepository installments;
  private final PaymentRepository payments;
  private final AuditService audit;

  public ContractService(
      ContractRepository contracts,
      ContractPartyRepository parties,
      ProjectRepository projects,
      UnitRepository units,
      CustomerRepository customers,
      ReservationRepository reservations,
      PaymentPlanRepository plans,
      InstallmentRepository installments,
      PaymentRepository payments,
      AuditService audit) {
    this.contracts = contracts;
    this.parties = parties;
    this.projects = projects;
    this.units = units;
    this.customers = customers;
    this.reservations = reservations;
    this.plans = plans;
    this.installments = installments;
    this.payments = payments;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public PageResponse<Contract> search(Contract.Status status, UUID projectId, int page, int size) {
    return PageResponse.of(contracts.search(SecuritySupport.tenantId(), status, projectId, Pages.desc(page, size)));
  }

  @Transactional(readOnly = true)
  public ContractDetail get(UUID id) {
    Contract contract = require(id);
    return detail(contract);
  }

  @Transactional
  public Contract create(CreateContract request) {
    UUID tenantId = SecuritySupport.tenantId();
    projects.findByIdAndTenantId(request.projectId(), tenantId).orElseThrow(() -> ApiException.badRequest("El proyecto no pertenece a la constructora"));
    Unit unit = units.lock(request.unitId(), tenantId).orElseThrow(() -> ApiException.notFound("La unidad no existe"));
    if (!unit.getProjectId().equals(request.projectId())) {
      throw ApiException.badRequest("La unidad no pertenece al proyecto");
    }
    if (unit.getAvailability() == Unit.Availability.SOLD || unit.getAvailability() == Unit.Availability.OFF_MARKET) {
      throw ApiException.conflict("La unidad no admite un contrato nuevo");
    }
    if (contracts.existsByUnitIdAndStatusIn(unit.getId(), List.of(Contract.Status.DRAFT, Contract.Status.ACTIVE))) {
      throw ApiException.conflict("La unidad ya tiene un contrato abierto");
    }
    if (request.agreedPrice() == null || request.agreedPrice().signum() <= 0) {
      throw ApiException.badRequest("El precio acordado debe ser mayor que cero");
    }
    BigDecimal down = request.agreedDownPayment() == null ? BigDecimal.ZERO : request.agreedDownPayment();
    if (down.signum() < 0 || down.compareTo(request.agreedPrice()) > 0) {
      throw ApiException.badRequest("La inicial no puede superar el precio acordado");
    }
    Contract contract = new Contract();
    contract.setTenantId(tenantId);
    contract.setProjectId(request.projectId());
    contract.setUnitId(request.unitId());
    contract.setAgreedPrice(request.agreedPrice());
    contract.setAgreedDownPayment(down);
    contract.setCurrency(normalizeCurrency(request.currency()));
    contract.setAgreementDate(request.agreementDate() == null ? LocalDate.now() : request.agreementDate());
    contract.setNotes(Texts.blankToNull(request.notes()));
    contract.setStatus(Contract.Status.DRAFT);
    contract.setCreatedBy(SecuritySupport.currentUserId());
    reservations.findByUnitIdAndStatus(unit.getId(), Reservation.Status.ACTIVE).ifPresent(reservation -> contract.setReservationId(reservation.getId()));
    contracts.save(contract);
    if (request.customerId() != null) {
      addParty(contract.getId(), request.customerId(), true);
    }
    audit.record("CONTRACT_CREATED", "CONTRACT", contract.getId(), "Creó un borrador de contrato", Map.of("unitId", unit.getId().toString()));
    return contract;
  }

  @Transactional
  public Contract update(UUID id, CreateContract request) {
    Contract contract = lockDraft(id);
    if (request.agreedPrice() == null || request.agreedPrice().signum() <= 0) {
      throw ApiException.badRequest("El precio acordado debe ser mayor que cero");
    }
    BigDecimal down = request.agreedDownPayment() == null ? BigDecimal.ZERO : request.agreedDownPayment();
    if (down.compareTo(request.agreedPrice()) > 0) {
      throw ApiException.badRequest("La inicial no puede superar el precio acordado");
    }
    contract.setAgreedPrice(request.agreedPrice());
    contract.setAgreedDownPayment(down);
    contract.setCurrency(normalizeCurrency(request.currency()));
    contract.setAgreementDate(request.agreementDate() == null ? contract.getAgreementDate() : request.agreementDate());
    contract.setNotes(Texts.blankToNull(request.notes()));
    return contract;
  }

  @Transactional
  public ContractParty addParty(UUID contractId, UUID customerId, boolean primary) {
    Contract contract = lockDraft(contractId);
    customers.findByIdAndTenantId(customerId, contract.getTenantId()).orElseThrow(() -> ApiException.notFound("El comprador no existe"));
    if (parties.existsByContractIdAndCustomerId(contractId, customerId)) {
      throw ApiException.conflict("El comprador ya está en el contrato");
    }
    if (primary) {
      parties.findByContractIdAndTenantId(contractId, contract.getTenantId()).forEach(party -> party.setPrimaryBuyer(false));
    }
    ContractParty party = new ContractParty();
    party.setTenantId(contract.getTenantId());
    party.setContractId(contractId);
    party.setCustomerId(customerId);
    party.setPrimaryBuyer(primary || parties.countByContractId(contractId) == 0);
    return parties.save(party);
  }

  @Transactional
  public void removeParty(UUID contractId, UUID customerId) {
    lockDraft(contractId);
    ContractParty party = parties.findByContractIdAndCustomerId(contractId, customerId)
        .orElseThrow(() -> ApiException.notFound("El comprador no está en el contrato"));
    parties.delete(party);
  }

  @Transactional
  public Contract activate(UUID id) {
    UUID tenantId = SecuritySupport.tenantId();
    Contract contract = contracts.lock(id, tenantId).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() != Contract.Status.DRAFT) {
      throw ApiException.conflict("Solo un borrador puede activarse");
    }
    if (parties.countByContractId(id) == 0) {
      throw ApiException.badRequest("Agregue al menos un comprador");
    }
    if (plans.findByContractIdAndTenantId(id, tenantId).isEmpty() || installments.findByContractIdAndTenantIdOrderBySequenceNumberAsc(id, tenantId).isEmpty()) {
      throw ApiException.badRequest("Configure el plan de pagos antes de activar el contrato");
    }
    Unit unit = units.lock(contract.getUnitId(), tenantId).orElseThrow(() -> ApiException.notFound("La unidad no existe"));
    if (unit.getAvailability() == Unit.Availability.SOLD || unit.getAvailability() == Unit.Availability.OFF_MARKET) {
      throw ApiException.conflict("La unidad no está disponible para la venta");
    }
    if (unit.getAvailability() == Unit.Availability.RESERVED) {
      Reservation reservation = reservations.findByUnitIdAndStatus(unit.getId(), Reservation.Status.ACTIVE)
          .orElseThrow(() -> ApiException.conflict("La unidad figura reservada sin una reserva activa"));
      boolean sameCustomer = reservation.getCustomerId() != null && parties.findByContractIdAndTenantId(id, tenantId).stream()
          .anyMatch(party -> party.getCustomerId().equals(reservation.getCustomerId()));
      boolean linked = contract.getReservationId() != null && contract.getReservationId().equals(reservation.getId());
      if (reservation.getCustomerId() != null && !sameCustomer) {
        throw ApiException.conflict("La reserva activa pertenece a otro comprador");
      }
      if (reservation.getCustomerId() == null && !linked) {
        throw ApiException.conflict("Confirme la reserva del prospecto antes de activar la venta");
      }
      reservation.setStatus(Reservation.Status.CONVERTED);
    }
    unit.setAvailability(Unit.Availability.SOLD);
    contract.setStatus(Contract.Status.ACTIVE);
    audit.record("CONTRACT_ACTIVATED", "CONTRACT", contract.getId(), "Activó el contrato de venta", Map.of("unitId", unit.getId().toString()));
    return contract;
  }

  @Transactional
  public Contract complete(UUID id) {
    Contract contract = contracts.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() != Contract.Status.ACTIVE) {
      throw ApiException.conflict("Solo un contrato activo puede completarse");
    }
    BigDecimal paid = payments.findByContractIdAndTenantIdOrderByCreatedAtDesc(id, contract.getTenantId()).stream()
        .filter(payment -> payment.getStatus() == Payment.Status.VALIDATED)
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    var plan = plans.findByContractIdAndTenantId(id, contract.getTenantId()).orElseThrow(() -> ApiException.badRequest("El contrato no tiene plan de pagos"));
    if (paid.compareTo(plan.getTotalPrice()) < 0) {
      throw ApiException.conflict("Aún hay saldo pendiente");
    }
    contract.setStatus(Contract.Status.COMPLETED);
    audit.record("CONTRACT_COMPLETED", "CONTRACT", contract.getId(), "Marcó el contrato como completado", Map.of());
    return contract;
  }

  @Transactional
  public Contract annul(UUID id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw ApiException.badRequest("Indique el motivo de la anulación");
    }
    UUID tenantId = SecuritySupport.tenantId();
    Contract contract = contracts.lock(id, tenantId).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() == Contract.Status.ANNULLED) {
      throw ApiException.conflict("El contrato ya está anulado");
    }
    if (payments.existsByContractIdAndStatus(id, Payment.Status.VALIDATED)) {
      throw ApiException.conflict("Revierta los pagos confirmados antes de anular el contrato");
    }
    contract.setStatus(Contract.Status.ANNULLED);
    contract.setAnnulledAt(Instant.now());
    contract.setAnnulmentReason(reason.trim());
    Unit unit = units.lock(contract.getUnitId(), tenantId).orElse(null);
    if (unit != null && unit.getAvailability() == Unit.Availability.SOLD
        && contracts.findByUnitIdAndStatus(unit.getId(), Contract.Status.ACTIVE).isEmpty()) {
      unit.setAvailability(Unit.Availability.AVAILABLE);
    }
    audit.record("CONTRACT_ANNULLED", "CONTRACT", contract.getId(), "Anuló el contrato", Map.of("reason", reason.trim()));
    return contract;
  }

  @Transactional(readOnly = true)
  public Contract require(UUID id) {
    return contracts.findByIdAndTenantId(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
  }

  @Transactional(readOnly = true)
  public ContractDetail detail(Contract contract) {
    return new ContractDetail(contract, parties.findByContractIdAndTenantId(contract.getId(), contract.getTenantId()));
  }

  private Contract lockDraft(UUID id) {
    Contract contract = contracts.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El contrato no existe"));
    if (contract.getStatus() != Contract.Status.DRAFT) {
      throw ApiException.conflict("El contrato ya no se puede modificar");
    }
    return contract;
  }

  private static String normalizeCurrency(String currency) {
    String value = currency == null || currency.isBlank() ? "DOP" : currency.trim().toUpperCase();
    if (!value.matches("^[A-Z]{3}$")) {
      throw ApiException.badRequest("La moneda debe ser un código de tres letras");
    }
    return value;
  }

  public record CreateContract(
      UUID projectId,
      UUID unitId,
      UUID customerId,
      BigDecimal agreedPrice,
      BigDecimal agreedDownPayment,
      String currency,
      LocalDate agreementDate,
      String notes) {}

  public record ContractDetail(Contract contract, List<ContractParty> parties) {}
}
