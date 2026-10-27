package rd.alzada.leads;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {

  List<LeadActivity> findByLeadIdAndTenantIdOrderByCreatedAtDesc(UUID leadId, UUID tenantId);
}
