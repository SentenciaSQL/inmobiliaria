package rd.alzada.notifications;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends TenantEntity {

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 60)
  private String type;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  private Instant readAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EmailStatus emailStatus = EmailStatus.PENDING;

  public enum EmailStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED
  }
}
