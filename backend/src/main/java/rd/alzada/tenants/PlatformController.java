package rd.alzada.tenants;

import rd.alzada.audit.AuditEvent;
import rd.alzada.identity.PasswordPolicy;
import rd.alzada.audit.AuditService;
import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import rd.alzada.notifications.NotificationTemplate;
import rd.alzada.reporting.DashboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
@PreAuthorize(SecurityRoles.PLATFORM)
public class PlatformController {

  private final PlatformService platform;
  private final AuditService audit;

  public PlatformController(PlatformService platform, AuditService audit) {
    this.platform = platform;
    this.audit = audit;
  }

  @GetMapping("/metrics")
  public DashboardService.PlatformDashboard metrics() {
    return platform.metrics();
  }

  @GetMapping("/companies")
  public PageResponse<CompanyService.CompanyResponse> companies(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Company.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return platform.companies(q, status, page, size);
  }

  @PostMapping("/companies")
  public CompanyService.CompanyResponse create(@Valid @RequestBody CreateBody body) {
    return platform.create(body.toCommand());
  }

  @PatchMapping("/companies/{id}/status")
  public CompanyService.CompanyResponse status(@PathVariable UUID id, @Valid @RequestBody StatusBody body) {
    return platform.status(id, body.status());
  }

  @GetMapping("/settings")
  public Map<String, String> settings() {
    return platform.settings();
  }

  @PutMapping("/settings")
  public Map<String, String> updateSettings(@Valid @RequestBody SettingsBody body) {
    return platform.updateSettings(body.platformName(), body.supportEmail(), body.tagline(), body.currency());
  }

  @GetMapping("/templates")
  public List<NotificationTemplate> templates() {
    return platform.templates();
  }

  @PutMapping("/templates/{code}")
  public NotificationTemplate updateTemplate(@PathVariable String code, @Valid @RequestBody TemplateBody body) {
    return platform.updateTemplate(code, body.subject(), body.body());
  }

  @GetMapping("/audit")
  public PageResponse<AuditEvent> audit(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return audit.platform(page, size);
  }

  public record CreateBody(
      @NotBlank @Size(max = 160) String commercialName,
      @NotBlank @Size(max = 200) String legalName,
      @Size(max = 20) String rnc,
      @Size(max = 30) String phone,
      @Email @Size(max = 180) String email,
      @Size(max = 240) String address,
      @Size(max = 2) String provinceCode,
      UUID municipalityId,
      @Size(max = 200) String website,
      @Size(max = 4000) String description,
      @Size(max = 30) String publicPhone,
      @Email @Size(max = 180) String publicEmail,
      @Size(max = 1000) String publicContact,
      @NotBlank @Size(max = 160) String adminFullName,
      @NotBlank @Email @Size(max = 180) String adminEmail,
      @Size(max = 30) String adminPhone,
      @NotBlank @PasswordPolicy String adminPassword) {

    CompanyService.CreateCompanyCommand toCommand() {
      return new CompanyService.CreateCompanyCommand(
          commercialName, legalName, rnc, phone, email, address, provinceCode, municipalityId, website, description,
          publicPhone, publicEmail, publicContact, adminFullName, adminEmail, adminPhone, adminPassword);
    }
  }

  public record StatusBody(@NotNull Company.Status status) {}

  public record SettingsBody(
      @NotBlank @Size(max = 80) String platformName,
      @Email @Size(max = 180) String supportEmail,
      @Size(max = 240) String tagline,
      @Size(min = 3, max = 3) String currency) {}

  public record TemplateBody(@NotBlank @Size(max = 180) String subject, @NotBlank String body) {}
}
