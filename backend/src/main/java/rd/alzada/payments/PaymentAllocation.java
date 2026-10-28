package rd.alzada.payments;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
public class PaymentAllocation extends TenantEntity {

  @Column(nullable = false)
  private UUID paymentId;

  @Column(nullable = false)
  private UUID installmentId;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private boolean voided;
}
