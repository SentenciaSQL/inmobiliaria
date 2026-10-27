package rd.alzada.identity;

import rd.alzada.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final AppProperties properties;
  private SecretKey key;

  public JwtService(AppProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void init() {
    String secret = properties.jwtSecret() == null ? "" : properties.jwtSecret();
    if (secret.length() < 32) {
      throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String issue(UUID userId, String email) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(properties.jwtAccessMinutes() * 60L);
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public UUID parseUserId(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return UUID.fromString(claims.getSubject());
  }
}
