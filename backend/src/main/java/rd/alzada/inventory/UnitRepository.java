package rd.alzada.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

  Optional<Unit> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Unit> findByProjectIdAndTenantIdOrderByCodeAsc(UUID projectId, UUID tenantId);

  boolean existsByProjectIdAndCodeIgnoreCase(UUID projectId, String code);

  long countByTenantIdAndAvailability(UUID tenantId, Unit.Availability availability);

  long countByBuildingId(UUID buildingId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select unit from Unit unit where unit.id = :id and unit.tenantId = :tenantId")
  Optional<Unit> lock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query("""
      select unit from Unit unit
      where unit.tenantId = :tenantId
        and (:projectId is null or unit.projectId = :projectId)
        and (:availability is null or unit.availability = :availability)
        and (:query is null or lower(unit.code) like :query escape '\\')
      """)
  Page<Unit> search(
      @Param("tenantId") UUID tenantId,
      @Param("projectId") UUID projectId,
      @Param("availability") Unit.Availability availability,
      @Param("query") String query,
      Pageable pageable);

  @Query("""
      select unit from Unit unit
      where unit.projectId = :projectId
        and unit.publiclyVisible = true
        and unit.availability <> rd.alzada.inventory.Unit.Availability.OFF_MARKET
      order by unit.code
      """)
  List<Unit> findVisible(UUID projectId);
}
