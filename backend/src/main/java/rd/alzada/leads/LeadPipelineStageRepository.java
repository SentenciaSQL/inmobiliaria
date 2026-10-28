package rd.alzada.leads;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadPipelineStageRepository extends JpaRepository<LeadPipelineStage, UUID> {

  List<LeadPipelineStage> findByTenantIdOrderBySortOrderAsc(UUID tenantId);

  Optional<LeadPipelineStage> findByTenantIdAndCode(UUID tenantId, String code);

  Optional<LeadPipelineStage> findByIdAndTenantId(UUID id, UUID tenantId);

  boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
