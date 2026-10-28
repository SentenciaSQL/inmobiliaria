package rd.alzada.tenants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "municipalities")
@Getter
@Setter
public class Municipality {

  @Id
  private UUID id;

  @Column(nullable = false, length = 2)
  private String provinceCode;

  @Column(nullable = false, length = 120)
  private String name;
}
