package rd.alzada.leads;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "leads")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Lead extends TenantEntity {

  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(length = 180)
  private String email;

  @Column(length = 30)
  private String phone;

  private UUID projectId;

  private UUID unitId;

  private UUID assignedUserId;

  @Column(nullable = false, length = 40)
  private String stageCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Source source;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(length = 240)
  private String nextStep;

  private LocalDate nextStepDate;

  private Instant deletedAt;

  public enum Source {
    MARKETPLACE,
    MANUAL,
    PHONE,
    REFERRAL,
    OTHER
  }
}
