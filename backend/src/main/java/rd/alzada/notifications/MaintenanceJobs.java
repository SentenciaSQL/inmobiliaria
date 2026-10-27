package rd.alzada.notifications;

import rd.alzada.common.TenantContext;
import rd.alzada.leads.CrmService;
import rd.alzada.payments.FinanceService;
import rd.alzada.payments.Installment;
import rd.alzada.reservations.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceJobs {

  private static final Logger log = LoggerFactory.getLogger(MaintenanceJobs.class);

  private final CrmService crm;
  private final FinanceService finance;

  public MaintenanceJobs(CrmService crm, FinanceService finance) {
    this.crm = crm;
    this.finance = finance;
  }

  @Scheduled(fixedDelay = 60000)
  public void reservations() {
    TenantContext.asSystem(null);
    try {
      for (Reservation reservation : crm.expiredReservations()) {
        runTenant(reservation.getTenantId(), () -> crm.expireOne(reservation.getId(), reservation.getTenantId()));
      }
      TenantContext.asSystem(null);
      for (Reservation reservation : crm.expiringReservations()) {
        runTenant(reservation.getTenantId(), () -> crm.noticeExpiring(reservation.getId(), reservation.getTenantId()));
      }
    } catch (RuntimeException exception) {
      log.warn("No se pudo completar el mantenimiento de reservas");
    } finally {
      TenantContext.clear();
    }
  }

  @Scheduled(cron = "0 15 * * * *", zone = "America/Santo_Domingo")
  public void installments() {
    TenantContext.asSystem(null);
    try {
      for (Installment installment : finance.newlyOverdue()) {
        runTenant(installment.getTenantId(), () -> finance.markOverdue(installment.getId(), installment.getTenantId()));
      }
      TenantContext.asSystem(null);
      for (Installment installment : finance.upcoming()) {
        runTenant(installment.getTenantId(), () -> finance.remind(installment.getId(), installment.getTenantId()));
      }
    } catch (RuntimeException exception) {
      log.warn("No se pudo completar el mantenimiento de cuotas");
    } finally {
      TenantContext.clear();
    }
  }

  private void runTenant(java.util.UUID tenantId, Runnable action) {
    TenantContext.asTenant(null, tenantId);
    try {
      action.run();
    } catch (RuntimeException exception) {
      log.warn("No se pudo procesar un registro de la constructora {}", tenantId);
    }
  }
}
