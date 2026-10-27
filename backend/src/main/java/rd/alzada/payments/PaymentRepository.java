package rd.alzada.payments;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Payment> findByContractIdAndTenantIdOrderByCreatedAtDesc(UUID contractId, UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select payment from Payment payment where payment.id = :id and payment.tenantId = :tenantId")
  Optional<Payment> lock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query("""
      select payment from Payment payment
      where payment.tenantId = :tenantId
        and (:status is null or payment.status = :status)
        and (:contractId is null or payment.contractId = :contractId)
      """)
  Page<Payment> search(
      @Param("tenantId") UUID tenantId,
      @Param("status") Payment.Status status,
      @Param("contractId") UUID contractId,
      Pageable pageable);

  @Query("""
      select coalesce(sum(payment.amount), 0) from Payment payment
      where payment.tenantId = :tenantId
        and payment.status = rd.alzada.payments.Payment.Status.VALIDATED
        and payment.validatedAt >= :from
        and payment.validatedAt < :to
      """)
  BigDecimal sumValidated(@Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);

  boolean existsByContractIdAndStatus(UUID contractId, Payment.Status status);
}
