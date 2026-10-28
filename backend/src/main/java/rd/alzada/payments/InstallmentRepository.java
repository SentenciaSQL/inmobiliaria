package rd.alzada.payments;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

  List<Installment> findByContractIdAndTenantIdOrderBySequenceNumberAsc(UUID contractId, UUID tenantId);

  void deleteByPlanIdAndTenantId(UUID planId, UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select installment from Installment installment
      where installment.contractId = :contractId and installment.tenantId = :tenantId
      order by installment.dueDate, installment.sequenceNumber
      """)
  List<Installment> lockByContract(@Param("contractId") UUID contractId, @Param("tenantId") UUID tenantId);

  @Query("""
      select installment from Installment installment
      where installment.status in (
          rd.alzada.payments.Installment.Status.PENDING,
          rd.alzada.payments.Installment.Status.PARTIAL)
        and installment.dueDate < :today
      """)
  List<Installment> findNewlyOverdue(@Param("today") LocalDate today);

  @Query("""
      select installment from Installment installment
      where installment.status in (
          rd.alzada.payments.Installment.Status.PENDING,
          rd.alzada.payments.Installment.Status.PARTIAL)
        and installment.upcomingNoticeSentAt is null
        and installment.dueDate >= :today
        and installment.dueDate <= :limit
      """)
  List<Installment> findUpcoming(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

  @Query("""
      select count(installment) from Installment installment
      where installment.tenantId = :tenantId
        and installment.status = rd.alzada.payments.Installment.Status.OVERDUE
      """)
  long countOverdue(UUID tenantId);

  @Query("select coalesce(max(installment.sequenceNumber), 0) from Installment installment where installment.planId = :planId")
  int maxSequence(UUID planId);
}
