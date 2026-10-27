package rd.alzada.leads;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

  Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

  @Query("""
      select lead from Lead lead
      where lead.tenantId = :tenantId
        and (:stage is null or lead.stageCode = :stage)
        and (:projectId is null or lead.projectId = :projectId)
        and (:assigned is null or lead.assignedUserId = :assigned or (:includeUnassigned = true and lead.assignedUserId is null))
        and (:query is null or lower(lead.fullName) like :query escape '\\' or lower(coalesce(lead.email, '')) like :query escape '\\')
      """)
  Page<Lead> search(
      @Param("tenantId") UUID tenantId,
      @Param("stage") String stage,
      @Param("projectId") UUID projectId,
      @Param("assigned") UUID assigned,
      @Param("includeUnassigned") boolean includeUnassigned,
      @Param("query") String query,
      Pageable pageable);

  @Query("""
      select count(lead) from Lead lead, LeadPipelineStage stage
      where lead.tenantId = :tenantId
        and stage.tenantId = lead.tenantId
        and stage.code = lead.stageCode
        and stage.closed = false
      """)
  long countOpen(UUID tenantId);
}
