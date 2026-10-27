package rd.alzada.identity;

import rd.alzada.common.SecuritySupport;
import rd.alzada.identity.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public SessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
    return SessionResponse.from(authService.login(request.email(), request.password(), http, response));
  }

  @PostMapping("/refresh")
  public SessionResponse refresh(HttpServletRequest http, HttpServletResponse response) {
    return SessionResponse.from(authService.refresh(http, response));
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest http, HttpServletResponse response) {
    authService.logout(http, response);
  }

  @GetMapping("/me")
  public UserResponse me() {
    return UserResponse.from(SecuritySupport.principal());
  }

  @PutMapping("/password")
  public void password(@Valid @RequestBody PasswordRequest request, HttpServletResponse response) {
    authService.changePassword(SecuritySupport.currentUserId(), request.currentPassword(), request.newPassword(), response);
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record PasswordRequest(@NotBlank String currentPassword, @NotBlank @PasswordPolicy String newPassword) {}

  public record SessionResponse(String accessToken, int expiresIn, UserResponse user) {
    static SessionResponse from(AuthService.AuthResult result) {
      return new SessionResponse(result.accessToken(), result.expiresIn(), UserResponse.from(result.principal()));
    }
  }

  public record UserResponse(
      String id,
      String email,
      String fullName,
      String phone,
      boolean platformAdmin,
      List<MembershipResponse> memberships) {
    static UserResponse from(UserPrincipal principal) {
      return new UserResponse(
          principal.id().toString(),
          principal.email(),
          principal.fullName(),
          principal.phone(),
          principal.platformAdmin(),
          principal.memberships().stream().map(MembershipResponse::from).toList());
    }
  }

  public record MembershipResponse(
      String id,
      String companyId,
      String companyName,
      String companySlug,
      String companyStatus,
      String logoUrl,
      String role) {
    static MembershipResponse from(MembershipView view) {
      return new MembershipResponse(
          view.id().toString(),
          view.companyId().toString(),
          view.companyName(),
          view.companySlug(),
          view.companyStatus(),
          view.hasLogo() ? "/api/v1/public/media/companies/" + view.companyId() + "/logo" : null,
          view.role());
    }
  }
}
