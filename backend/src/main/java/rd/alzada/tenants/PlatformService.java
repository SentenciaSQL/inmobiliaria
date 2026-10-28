package rd.alzada.tenants;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.Texts;
import rd.alzada.notifications.NotificationTemplate;
import rd.alzada.notifications.NotificationTemplateRepository;
import rd.alzada.reporting.DashboardService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {

  private final CompanyRepository companies;
  private final CompanyService companyService;
  private final PlatformSettingRepository settings;
  private final NotificationTemplateRepository templates;
  private final DashboardService dashboard;
  private final AuditService audit;

  public PlatformService(
      CompanyRepository companies,
      CompanyService companyService,
      PlatformSettingRepository settings,
      NotificationTemplateRepository templates,
      DashboardService dashboard,
      AuditService audit) {
    this.companies = companies;
    this.companyService = companyService;
    this.settings = settings;
    this.templates = templates;
    this.dashboard = dashboard;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public DashboardService.PlatformDashboard metrics() {
    return dashboard.platform();
  }

  @Transactional(readOnly = true)
  public PageResponse<CompanyService.CompanyResponse> companies(String query, Company.Status status, int page, int size) {
    return PageResponse.of(companies.search(Texts.like(query), status, Pages.desc(page, size)).map(companyService::toResponse));
  }

  @Transactional
  public CompanyService.CompanyResponse create(CompanyService.CreateCompanyCommand command) {
    return companyService.toResponse(companyService.provision(command));
  }

  @Transactional
  public CompanyService.CompanyResponse status(UUID id, Company.Status status) {
    Company company = companies.findById(id).orElseThrow(() -> ApiException.notFound("La constructora no existe"));
    company.setStatus(status);
    audit.record("COMPANY_STATUS", "COMPANY", company.getId(), "Cambió el estado de la constructora a " + status.name(), Map.of("status", status.name()));
    return companyService.toResponse(company);
  }

  @Transactional(readOnly = true)
  public Map<String, String> settings() {
    return settings.findAll().stream().collect(java.util.stream.Collectors.toMap(PlatformSetting::getKey, PlatformSetting::getValue));
  }

  @Transactional
  public Map<String, String> updateSettings(String platformName, String supportEmail, String tagline, String currency) {
    save("platform_name", required(platformName, "Indique el nombre de la plataforma"));
    save("support_email", Texts.lowerEmail(supportEmail) == null ? "soporte@alzada.do" : Texts.lowerEmail(supportEmail));
    save("marketplace_tagline", required(tagline, "Indique el texto del marketplace"));
    String code = currency == null ? "DOP" : currency.trim().toUpperCase();
    if (!code.matches("^[A-Z]{3}$")) {
      throw ApiException.badRequest("La moneda predeterminada no es válida");
    }
    save("default_currency", code);
    audit.record("SETTINGS_UPDATED", "PLATFORM", null, "Actualizó la configuración global", Map.of());
    return settings();
  }

  @Transactional(readOnly = true)
  public java.util.List<NotificationTemplate> templates() {
    return templates.findAllByOrderByCodeAsc();
  }

  @Transactional
  public NotificationTemplate updateTemplate(String code, String subject, String body) {
    NotificationTemplate template = templates.findById(code).orElseThrow(() -> ApiException.notFound("La plantilla no existe"));
    template.setSubject(required(subject, "Indique el asunto"));
    template.setBody(required(body, "Indique el contenido"));
    template.setUpdatedAt(Instant.now());
    return template;
  }

  private void save(String key, String value) {
    PlatformSetting setting = settings.findById(key).orElseGet(() -> {
      PlatformSetting created = new PlatformSetting();
      created.setKey(key);
      return created;
    });
    setting.setValue(value);
    setting.setUpdatedAt(Instant.now());
    settings.save(setting);
  }

  private static String required(String value, String message) {
    if (value == null || value.isBlank()) {
      throw ApiException.badRequest(message);
    }
    return value.trim();
  }
}
