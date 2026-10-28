package rd.alzada.identity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
    UUID id,
    String email,
    String fullName,
    String phone,
    boolean platformAdmin,
    List<MembershipView> memberships,
    Collection<? extends GrantedAuthority> authorities) implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public UserPrincipal withAuthorities(Collection<? extends GrantedAuthority> updated) {
    return new UserPrincipal(id, email, fullName, phone, platformAdmin, memberships, updated);
  }
}
