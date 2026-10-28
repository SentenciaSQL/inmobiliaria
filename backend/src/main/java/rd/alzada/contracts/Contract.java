package rd.alzada.contracts;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contracts")
@Getter
@Setter
public class Contract extends TenantEntity {

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false)
  private UUID unitId;

  private UUID reservationId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal agreedPrice;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Column(nullable = false)
  private LocalDate agreementDate;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal agreedDownPayment = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(columnDefinition = "text")
  private String notes;

  private UUID createdBy;

  private Instant annulledAt;

  @Column(length = 400)
  private String annulmentReason;

  public enum Status {
    DRAFT,
    ACTIVE,
    COMPLETED,
    ANNULLED
  }
}
