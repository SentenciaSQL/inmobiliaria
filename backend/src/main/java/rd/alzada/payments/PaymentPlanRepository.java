package rd.alzada.payments;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, UUID> {

  Optional<PaymentPlan> findByContractIdAndTenantId(UUID contractId, UUID tenantId);

  void deleteByContractIdAndTenantId(UUID contractId, UUID tenantId);
}
