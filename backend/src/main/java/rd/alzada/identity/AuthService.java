package rd.alzada.identity;

import rd.alzada.common.ApiException;
import rd.alzada.common.RateLimiter;
import rd.alzada.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  public static final String REFRESH_COOKIE = "alzada_refresh";

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final UserAccountService accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AppProperties properties;
  private final RateLimiter rateLimiter = new RateLimiter();
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      UserAccountService accounts,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AppProperties properties) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.properties = properties;
  }

  @Transactional
  public AuthResult login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
    String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    if (!rateLimiter.tryAcquire("login:" + ip, 10, Duration.ofMinutes(1))) {
      throw ApiException.badRequest("Demasiados intentos. Espere un minuto e intente de nuevo");
    }
    User user = users.findByEmailIgnoreCase(email.trim().toLowerCase())
        .filter(found -> found.getStatus() == User.Status.ACTIVE)
        .orElseThrow(() -> ApiException.unauthorized("Correo o contraseña incorrectos"));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw ApiException.unauthorized("Correo o contraseña incorrectos");
    }
    user.setLastLoginAt(Instant.now());
    return issue(user, response);
  }

  @Transactional
  public AuthResult refresh(HttpServletRequest request, HttpServletResponse response) {
    String raw = readCookie(request);
    if (raw == null) {
      throw ApiException.unauthorized("La sesión expiró");
    }
    RefreshToken current = refreshTokens.findByTokenHash(sha256(raw)).orElseThrow(() -> ApiException.unauthorized("La sesión expiró"));
    if (current.getRevokedAt() != null) {
      if (current.getReplacedById() != null) {
        revokeUser(current.getUserId());
      }
      throw ApiException.unauthorized("La sesión expiró");
    }
    if (current.getExpiresAt().isBefore(Instant.now())) {
      throw ApiException.unauthorized("La sesión expiró");
    }
    User user = users.findById(current.getUserId())
        .filter(found -> found.getStatus() == User.Status.ACTIVE)
        .orElseThrow(() -> ApiException.unauthorized("La sesión expiró"));
    current.setRevokedAt(Instant.now());
    AuthResult result = issue(user, response);
    current.setReplacedById(result.refreshTokenId());
    return result;
  }

  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String raw = readCookie(request);
    if (raw != null) {
      refreshTokens.findByTokenHash(sha256(raw)).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }
    clearCookie(response);
  }

  @Transactional
  public void changePassword(UUID userId, String currentPassword, String newPassword, HttpServletResponse response) {
    User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("La sesión no es válida"));
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw ApiException.badRequest("La contraseña actual no coincide");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    revokeUser(userId);
    clearCookie(response);
  }

  private AuthResult issue(User user, HttpServletResponse response) {
    String raw = newRawToken();
    RefreshToken token = new RefreshToken();
    token.setUserId(user.getId());
    token.setTokenHash(sha256(raw));
    token.setExpiresAt(Instant.now().plus(Duration.ofDays(properties.jwtRefreshDays())));
    refreshTokens.save(token);
    ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, raw)
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(Duration.ofDays(properties.jwtRefreshDays()))
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    UserPrincipal principal = accounts.principal(user, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    return new AuthResult(jwtService.issue(user.getId(), user.getEmail()), properties.jwtAccessMinutes() * 60, principal, token.getId());
  }

  private void revokeUser(UUID userId) {
    Instant now = Instant.now();
    refreshTokens.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.setRevokedAt(now));
  }

  private void clearCookie(HttpServletResponse response) {
    ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(0)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private String newRawToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String sha256(String raw) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo proteger la sesión");
    }
  }

  private static String readCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (var cookie : request.getCookies()) {
      if (REFRESH_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }

  public record AuthResult(String accessToken, int expiresIn, UserPrincipal principal, UUID refreshTokenId) {}
}
