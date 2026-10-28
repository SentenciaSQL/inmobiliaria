package rd.alzada.customers;

import rd.alzada.common.ApiException;
import rd.alzada.common.SecuritySupport;
import rd.alzada.contracts.Contract;
import rd.alzada.contracts.ContractRepository;
import rd.alzada.documents.DocumentFile;
import rd.alzada.documents.DocumentRepository;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.payments.FinanceService;
import rd.alzada.payments.Payment;
import rd.alzada.projects.Project;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.tenants.Company;
import rd.alzada.tenants.CompanyRepository;
import rd.alzada.tenants.CompanyService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BuyerService {

  private final CustomerRepository customers;
  private final ContractRepository contracts;
  private final ProjectRepository projects;
  private final UnitRepository units;
  private final CompanyRepository companies;
  private final DocumentRepository documents;
  private final FinanceService finance;

  public BuyerService(
      CustomerRepository customers,
      ContractRepository contracts,
      ProjectRepository projects,
      UnitRepository units,
      CompanyRepository companies,
      DocumentRepository documents,
      FinanceService finance) {
    this.customers = customers;
    this.contracts = contracts;
    this.projects = projects;
    this.units = units;
    this.companies = companies;
    this.documents = documents;
    this.finance = finance;
  }

  @Transactional(readOnly = true)
  public BuyerHome home() {
    Customer customer = customer();
    Company company = companies.findById(SecuritySupport.tenantId()).orElseThrow();
    if (customer == null) {
      return new BuyerHome(null, CompanyService.logoUrl(company), company.getCommercialName(), company.getPublicPhone(), company.getPublicEmail(), List.of());
    }
    List<BuyerOperation> operations = new ArrayList<>();
    for (Contract contract : contracts.findForBuyer(company.getId(), SecuritySupport.currentUserId())) {
      operations.add(operation(contract, false));
    }
    return new BuyerHome(customer.getFullName(), CompanyService.logoUrl(company), company.getCommercialName(), company.getPublicPhone(), company.getPublicEmail(), operations);
  }

  @Transactional(readOnly = true)
  public BuyerOperation operation(UUID contractId) {
    Contract contract = ownContract(contractId);
    return operation(contract, true);
  }

  @Transactional
  public Payment report(UUID contractId, java.math.BigDecimal amount, String reference, String notes, MultipartFile receipt) {
    ownContract(contractId);
    return finance.reportByBuyer(contractId, amount, reference, notes, receipt);
  }

  @Transactional(readOnly = true)
  public List<DocumentFile> documents() {
    Customer customer = customer();
    if (customer == null) {
      return List.of();
    }
    List<DocumentFile> visible = new ArrayList<>();
    for (Contract contract : contracts.findForBuyer(SecuritySupport.tenantId(), SecuritySupport.currentUserId())) {
      documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtDesc(contract.getTenantId(), DocumentFile.OwnerType.CONTRACT, contract.getId())
          .stream().filter(document -> document.getVisibility() == DocumentFile.Visibility.BUYER_VISIBLE).forEach(visible::add);
    }
    return visible;
  }

  private BuyerOperation operation(Contract contract, boolean detailed) {
    Project project = projects.findByIdAndTenantId(contract.getProjectId(), contract.getTenantId()).orElse(null);
    Unit unit = units.findByIdAndTenantId(contract.getUnitId(), contract.getTenantId()).orElse(null);
    FinanceService.PlanView plan = detailed || true ? finance.plan(contract.getId()) : null;
    return new BuyerOperation(
        contract.getId(),
        contract.getStatus(),
        contract.getAgreedPrice(),
        contract.getAgreedDownPayment(),
        contract.getCurrency(),
        contract.getAgreementDate(),
        project == null ? null : project.getName(),
        project == null ? null : project.getSlug(),
        unit == null ? null : unit.getCode(),
        unit == null ? null : unit.getPropertyType().name(),
        plan);
  }

  private Contract ownContract(UUID contractId) {
    return contracts.findForBuyer(SecuritySupport.tenantId(), SecuritySupport.currentUserId()).stream()
        .filter(contract -> contract.getId().equals(contractId))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("La operación no está disponible"));
  }

  private Customer customer() {
    return customers.findByTenantIdAndUserId(SecuritySupport.tenantId(), SecuritySupport.currentUserId()).orElse(null);
  }

  public record BuyerHome(
      String customerName,
      String logoUrl,
      String companyName,
      String publicPhone,
      String publicEmail,
      List<BuyerOperation> operations) {}

  public record BuyerOperation(
      UUID contractId,
      Contract.Status status,
      java.math.BigDecimal agreedPrice,
      java.math.BigDecimal agreedDownPayment,
      String currency,
      java.time.LocalDate agreementDate,
      String projectName,
      String projectSlug,
      String unitCode,
      String propertyType,
      FinanceService.PlanView plan) {}
}
