package rd.alzada.contracts;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contract_parties")
@Getter
@Setter
public class ContractParty extends TenantEntity {

  @Column(nullable = false)
  private UUID contractId;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private boolean primaryBuyer;
}
