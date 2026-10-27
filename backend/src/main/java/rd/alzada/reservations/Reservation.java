package rd.alzada.reservations;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservation extends TenantEntity {

  @Column(nullable = false)
  private UUID unitId;

  private UUID leadId;

  private UUID customerId;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  private UUID responsibleUserId;

  @Column(columnDefinition = "text")
  private String notes;

  private Instant expiryNoticeSentAt;

  public enum Status {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    CONVERTED
  }
}
