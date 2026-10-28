package rd.alzada.identity;

import rd.alzada.common.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

  private final UserRepository users;

  @PersistenceContext
  private EntityManager entityManager;

  public UserAccountService(UserRepository users) {
    this.users = users;
  }

  @Transactional(readOnly = true)
  public UserPrincipal load(UUID userId) {
    User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("La sesión no es válida"));
    if (user.getStatus() != User.Status.ACTIVE) {
      throw ApiException.unauthorized("La sesión no es válida");
    }
    return principal(user, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Transactional(readOnly = true)
  public UserPrincipal principal(User user, List<SimpleGrantedAuthority> authorities) {
    return new UserPrincipal(
        user.getId(),
        user.getEmail(),
        user.getFullName(),
        user.getPhone(),
        user.isPlatformAdmin(),
        membershipsOf(user.getId()),
        authorities);
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<MembershipView> membershipsOf(UUID userId) {
    List<Object[]> rows = entityManager.createNativeQuery("""
        select id, company_id, role, status, company_name, company_slug, company_status, has_logo
        from app_user_memberships(?1)
        """).setParameter(1, userId).getResultList();
    List<MembershipView> views = new ArrayList<>();
    for (Object[] row : rows) {
      views.add(new MembershipView(
          (UUID) row[0],
          (UUID) row[1],
          row[4] == null ? null : row[4].toString(),
          row[5] == null ? null : row[5].toString(),
          row[6] == null ? null : row[6].toString(),
          row[7] instanceof Boolean value && value,
          row[2] == null ? null : row[2].toString()));
    }
    return views;
  }
}
