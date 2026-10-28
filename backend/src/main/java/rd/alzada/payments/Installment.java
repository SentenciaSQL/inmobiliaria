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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "installments")
@Getter
@Setter
public class Installment extends TenantEntity {

  @Column(nullable = false)
  private UUID planId;

  @Column(nullable = false)
  private UUID contractId;

  @Column(nullable = false)
  private int sequenceNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private Kind kind;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  private Instant upcomingNoticeSentAt;

  private Instant overdueNoticeSentAt;

  @Version
  @Column(nullable = false)
  private long rowVersion;

  public enum Kind {
    DOWN_PAYMENT,
    REGULAR,
    EXTRAORDINARY,
    DELIVERY
  }

  public enum Status {
    PENDING,
    PARTIAL,
    PAID,
    OVERDUE,
    CANCELLED
  }
}
