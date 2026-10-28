package rd.alzada.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

  @Modifying
  @Query("update RefreshToken token set token.revokedAt = CURRENT_TIMESTAMP where token.userId = :userId and token.revokedAt is null")
  void revokeAll(UUID userId);
}
