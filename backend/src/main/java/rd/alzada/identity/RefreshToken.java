package rd.alzada.identity;

import rd.alzada.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken extends BaseEntity {

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant revokedAt;

  private UUID replacedById;
}
