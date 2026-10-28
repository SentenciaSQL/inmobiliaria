package rd.alzada.common;

import rd.alzada.identity.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecuritySupport {

  private SecuritySupport() {}

  public static UserPrincipal principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw ApiException.unauthorized("Debe iniciar sesión");
    }
    return principal;
  }

  public static UUID currentUserId() {
    return principal().id();
  }

  public static UUID tenantId() {
    UUID tenantId = TenantContext.tenantId();
    if (tenantId == null) {
      throw ApiException.companyRequired();
    }
    return tenantId;
  }

  public static boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    String authority = "ROLE_" + role;
    return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
  }
}
