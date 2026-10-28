package rd.alzada.payments;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends TenantEntity {

  @Column(nullable = false)
  private UUID contractId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  private UUID reportedBy;

  private UUID validatedBy;

  private Instant validatedAt;

  @Column(length = 400)
  private String rejectionReason;

  private UUID reversedBy;

  private Instant reversedAt;

  @Column(length = 400)
  private String reversalReason;

  @Column(length = 80)
  private String referenceNumber;

  @Column(columnDefinition = "text")
  private String notes;

  private UUID receiptDocumentId;

  @Version
  @Column(nullable = false)
  private long rowVersion;

  public enum Status {
    REPORTED,
    VALIDATED,
    REJECTED,
    REVERSED
  }
}
