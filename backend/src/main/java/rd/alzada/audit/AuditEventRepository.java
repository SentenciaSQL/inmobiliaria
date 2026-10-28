package rd.alzada.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  Page<AuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

  @Query("select event from AuditEvent event where event.tenantId is null order by event.createdAt desc")
  Page<AuditEvent> platform(Pageable pageable);
}
