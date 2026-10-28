package rd.alzada.inventory;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "buildings")
@Getter
@Setter
public class Building extends TenantEntity {

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 40)
  private String code;

  private Integer floors;
}
