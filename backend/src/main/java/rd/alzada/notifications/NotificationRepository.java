package rd.alzada.notifications;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(UUID userId, UUID tenantId, Pageable pageable);

  Optional<Notification> findByIdAndUserIdAndTenantId(UUID id, UUID userId, UUID tenantId);

  long countByUserIdAndTenantIdAndReadAtIsNull(UUID userId, UUID tenantId);

  @Modifying
  @Query("update Notification notification set notification.readAt = :now where notification.userId = :userId and notification.tenantId = :tenantId and notification.readAt is null")
  int markAllRead(UUID userId, UUID tenantId, Instant now);
}
