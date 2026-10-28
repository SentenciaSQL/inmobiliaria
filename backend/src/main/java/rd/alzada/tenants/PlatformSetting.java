package rd.alzada.tenants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSetting {

  @Id
  @Column(name = "setting_key", length = 80)
  private String key;

  @Column(name = "setting_value", nullable = false, columnDefinition = "text")
  private String value;

  @Column(nullable = false)
  private Instant updatedAt;
}
