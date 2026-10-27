package rd.alzada.leads;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lead_activities")
@Getter
@Setter
public class LeadActivity extends TenantEntity {

  @Column(nullable = false)
  private UUID leadId;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false, length = 32)
  private Type type;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  private UUID createdBy;

  public enum Type {
    NOTE,
    CALL,
    VISIT,
    EMAIL,
    STATUS_CHANGE,
    WHATSAPP
  }
}
