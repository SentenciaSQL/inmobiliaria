package rd.alzada.projects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {

  List<ProjectImage> findByProjectIdOrderBySortOrderAsc(UUID projectId);

  Optional<ProjectImage> findByIdAndProjectIdAndTenantId(UUID id, UUID projectId, UUID tenantId);

  @Query("select coalesce(max(image.sortOrder), -1) from ProjectImage image where image.projectId = :projectId")
  int maxSortOrder(UUID projectId);

  long countByProjectId(UUID projectId);
}
