package rd.alzada.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
public class NotificationTemplate {

  @Id
  @Column(length = 60)
  private String code;

  @Column(nullable = false, length = 240)
  private String description;

  @Column(nullable = false, length = 180)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(nullable = false)
  private Instant updatedAt;
}
