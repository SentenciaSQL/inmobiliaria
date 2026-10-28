package rd.alzada.tenants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "provinces")
@Getter
@Setter
public class Province {

  @Id
  @Column(length = 2)
  private String code;

  @Column(nullable = false, length = 80)
  private String name;
}
