package rd.alzada.notifications;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

  private final NotificationService notifications;

  public NotificationController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @GetMapping("/api/v1/notifications")
  @PreAuthorize("isAuthenticated()")
  public PageResponse<Notification> inbox(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return notifications.inbox(page, size);
  }

  @GetMapping("/api/v1/notifications/unread")
  @PreAuthorize("isAuthenticated()")
  public UnreadCount unread() {
    return new UnreadCount(notifications.unread());
  }

  @PostMapping("/api/v1/notifications/{id}/read")
  @PreAuthorize("isAuthenticated()")
  public void markRead(@PathVariable UUID id) {
    notifications.markRead(id);
  }

  @PostMapping("/api/v1/notifications/read-all")
  @PreAuthorize("isAuthenticated()")
  public void markAllRead() {
    notifications.markAllRead();
  }

  @GetMapping("/api/v1/company/notification-templates")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public List<NotificationService.CompanyTemplateView> templates() {
    return notifications.companyTemplates();
  }

  @PutMapping("/api/v1/company/notification-templates/{code}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public NotificationService.CompanyTemplateView save(@PathVariable String code, @Valid @RequestBody TemplateBody body) {
    return notifications.saveCompanyTemplate(code, body.subject(), body.body(), body.enabled());
  }

  @DeleteMapping("/api/v1/company/notification-templates/{code}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void reset(@PathVariable String code) {
    notifications.resetCompanyTemplate(code);
  }

  public record UnreadCount(long count) {}

  public record TemplateBody(@NotBlank @Size(max = 180) String subject, @NotBlank String body, boolean enabled) {}
}
