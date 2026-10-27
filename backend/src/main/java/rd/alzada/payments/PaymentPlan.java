package rd.alzada.payments;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_plans")
@Getter
@Setter
public class PaymentPlan extends TenantEntity {

  @Column(nullable = false, unique = true)
  private UUID contractId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal totalPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal downPayment = BigDecimal.ZERO;

  @Column(nullable = false)
  private int installmentCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Frequency frequency;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Column(precision = 14, scale = 2)
  private BigDecimal deliveryAmount;

  @Column(columnDefinition = "text")
  private String conditions;

  public enum Frequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY
  }
}
