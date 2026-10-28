package rd.alzada.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import rd.alzada.common.TenantContext;
import rd.alzada.identity.MembershipView;
import rd.alzada.identity.UserPrincipal;
import rd.alzada.tenants.Company;
import rd.alzada.tenants.CompanyRepository;
import rd.alzada.tenants.CompanyRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantContextFilter extends OncePerRequestFilter {

  private final CompanyRepository companies;
  private final ObjectMapper objectMapper;

  public TenantContextFilter(CompanyRepository companies, ObjectMapper objectMapper) {
    this.companies = companies;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      String path = request.getRequestURI();
      UserPrincipal principal = currentPrincipal();
      if (isOpen(path)) {
        TenantContext.asMarketplace(principal == null ? null : principal.id());
        chain.doFilter(request, response);
        return;
      }
      if (path.startsWith("/api/v1/platform")) {
        if (principal != null && principal.platformAdmin()) {
          TenantContext.asSystem(principal.id());
          authorize(principal, "PLATFORM_ADMIN");
        }
        chain.doFilter(request, response);
        return;
      }
      if (principal == null) {
        chain.doFilter(request, response);
        return;
      }
      List<MembershipView> memberships = principal.memberships().stream()
          .filter(item -> "ACTIVE".equals(item.companyStatus()))
          .toList();
      MembershipView selected = select(request, memberships);
      if (selected == null) {
        if (roleMismatch(request, memberships)) {
          write(response, 403, "FORBIDDEN", "No tiene ese rol en la constructora");
          return;
        }
        if (foreignCompany(request, memberships)) {
          write(response, 403, "FORBIDDEN", "No tiene acceso a esa constructora");
          return;
        }
        write(response, 409, "COMPANY_REQUIRED", "Seleccione la constructora activa");
        return;
      }
      try {
        TenantContext.asTenant(principal.id(), selected.companyId());
        Company company = companies.findById(selected.companyId()).orElse(null);
        if (company == null || company.getStatus() != Company.Status.ACTIVE) {
          write(response, 403, "FORBIDDEN", "La constructora está suspendida");
          return;
        }
      } catch (RuntimeException exception) {
        write(response, 403, "FORBIDDEN", "No se pudo validar la constructora activa");
        return;
      }
      authorize(principal, selected.role());
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private static boolean isOpen(String path) {
    return path.startsWith("/api/v1/public")
        || path.startsWith("/api/v1/auth")
        || path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.equals("/sitemap.xml");
  }

  private static UserPrincipal currentPrincipal() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return principal;
    }
    return null;
  }

  private static MembershipView select(HttpServletRequest request, List<MembershipView> memberships) {
    String requestedCompany = request.getHeader("X-Company-Id");
    String requestedRole = request.getHeader("X-Company-Role");
    List<MembershipView> candidates = memberships;
    if (requestedCompany != null && !requestedCompany.isBlank()) {
      UUID companyId;
      try {
        companyId = UUID.fromString(requestedCompany.trim());
      } catch (IllegalArgumentException exception) {
        return null;
      }
      candidates = memberships.stream().filter(item -> item.companyId().equals(companyId)).toList();
      if (candidates.isEmpty()) {
        return null;
      }
    } else if (memberships.size() != 1 && memberships.stream().map(MembershipView::companyId).distinct().count() != 1) {
      return null;
    }
    if (requestedRole != null && !requestedRole.isBlank()) {
      return candidates.stream().filter(item -> item.role().equals(requestedRole.trim())).findFirst().orElse(null);
    }
    CompanyRole highest = CompanyRole.highest(candidates.stream().map(item -> CompanyRole.valueOf(item.role())).toList());
    return candidates.stream().filter(item -> item.role().equals(highest.name())).findFirst().orElse(candidates.get(0));
  }

  private static boolean foreignCompany(HttpServletRequest request, List<MembershipView> memberships) {
    String requestedCompany = request.getHeader("X-Company-Id");
    if (requestedCompany == null || requestedCompany.isBlank()) {
      return false;
    }
    UUID companyId;
    try {
      companyId = UUID.fromString(requestedCompany.trim());
    } catch (IllegalArgumentException exception) {
      return true;
    }
    return memberships.stream().noneMatch(item -> item.companyId().equals(companyId));
  }

  private static boolean roleMismatch(HttpServletRequest request, List<MembershipView> memberships) {
    String requestedRole = request.getHeader("X-Company-Role");
    if (requestedRole == null || requestedRole.isBlank()) {
      return false;
    }
    String requestedCompany = request.getHeader("X-Company-Id");
    List<MembershipView> candidates = memberships;
    if (requestedCompany != null && !requestedCompany.isBlank()) {
      UUID companyId;
      try {
        companyId = UUID.fromString(requestedCompany.trim());
      } catch (IllegalArgumentException exception) {
        return false;
      }
      candidates = memberships.stream().filter(item -> item.companyId().equals(companyId)).toList();
    }
    return !candidates.isEmpty() && candidates.stream().noneMatch(item -> item.role().equals(requestedRole.trim()));
  }

  private static void authorize(UserPrincipal principal, String role) {
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    UserPrincipal updated = principal.withAuthorities(authorities);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(updated, null, authorities));
  }

  private void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    body.put("detail", detail);
    body.put("code", code);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
