package rd.alzada.audit;

import rd.alzada.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
public class AuditEvent extends BaseEntity {

  private UUID tenantId;

  private UUID actorId;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(nullable = false, length = 60)
  private String entityType;

  private UUID entityId;

  @Column(nullable = false, length = 400)
  private String summary;

  @Column(columnDefinition = "text")
  private String details;

  @Column(length = 64)
  private String ipAddress;
}
