package rd.alzada.reporting;

import rd.alzada.common.SecuritySupport;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.leads.LeadRepository;
import rd.alzada.payments.InstallmentRepository;
import rd.alzada.payments.PaymentRepository;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.reservations.ReservationRepository;
import rd.alzada.tenants.Company;
import rd.alzada.tenants.CompanyRepository;
import rd.alzada.identity.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private static final ZoneId SANTO_DOMINGO = ZoneId.of("America/Santo_Domingo");

  private final ProjectRepository projects;
  private final UnitRepository units;
  private final LeadRepository leads;
  private final ReservationRepository reservations;
  private final InstallmentRepository installments;
  private final PaymentRepository payments;
  private final CompanyRepository companies;
  private final UserRepository users;

  public DashboardService(
      ProjectRepository projects,
      UnitRepository units,
      LeadRepository leads,
      ReservationRepository reservations,
      InstallmentRepository installments,
      PaymentRepository payments,
      CompanyRepository companies,
      UserRepository users) {
    this.projects = projects;
    this.units = units;
    this.leads = leads;
    this.reservations = reservations;
    this.installments = installments;
    this.payments = payments;
    this.companies = companies;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public CompanyDashboard company(LocalDate from, LocalDate to) {
    LocalDate today = LocalDate.now(SANTO_DOMINGO);
    LocalDate start = from == null ? today.withDayOfMonth(1) : from;
    LocalDate end = to == null ? today : to;
    if (end.isBefore(start)) {
      end = start;
    }
    var tenantId = SecuritySupport.tenantId();
    Instant fromInstant = start.atStartOfDay(SANTO_DOMINGO).toInstant();
    Instant toInstant = end.plusDays(1).atStartOfDay(SANTO_DOMINGO).toInstant();
    Instant now = Instant.now();
    return new CompanyDashboard(
        projects.countByTenantId(tenantId),
        units.countByTenantIdAndAvailability(tenantId, Unit.Availability.AVAILABLE),
        units.countByTenantIdAndAvailability(tenantId, Unit.Availability.RESERVED),
        units.countByTenantIdAndAvailability(tenantId, Unit.Availability.SOLD),
        leads.countOpen(tenantId),
        reservations.countExpiring(tenantId, now, now.plusSeconds(48L * 3600)),
        installments.countOverdue(tenantId),
        payments.sumValidated(tenantId, fromInstant, toInstant),
        "DOP",
        start,
        end);
  }

  @Transactional(readOnly = true)
  public PlatformDashboard platform() {
    return new PlatformDashboard(
        companies.countByStatus(Company.Status.ACTIVE),
        companies.countByStatus(Company.Status.SUSPENDED),
        projects.countByPublicationStatus(rd.alzada.projects.Project.PublicationStatus.PUBLISHED),
        users.count());
  }

  public record CompanyDashboard(
      long projects,
      long availableUnits,
      long reservedUnits,
      long soldUnits,
      long openLeads,
      long reservationsExpiringSoon,
      long overdueInstallments,
      BigDecimal collections,
      String currency,
      LocalDate from,
      LocalDate to) {}

  public record PlatformDashboard(long activeCompanies, long suspendedCompanies, long publishedProjects, long users) {}
}
