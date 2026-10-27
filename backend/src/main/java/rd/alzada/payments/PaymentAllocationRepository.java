package rd.alzada.payments;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

  List<PaymentAllocation> findByPaymentIdAndTenantId(UUID paymentId, UUID tenantId);

  List<PaymentAllocation> findByInstallmentIdAndVoidedFalse(UUID installmentId);
}
