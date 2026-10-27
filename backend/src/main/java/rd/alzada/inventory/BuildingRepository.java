package rd.alzada.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

  List<Building> findByProjectIdAndTenantIdOrderByNameAsc(UUID projectId, UUID tenantId);

  Optional<Building> findByIdAndTenantId(UUID id, UUID tenantId);

  boolean existsByProjectIdAndCodeIgnoreCase(UUID projectId, String code);

  long countByIdAndProjectId(UUID id, UUID projectId);
}
