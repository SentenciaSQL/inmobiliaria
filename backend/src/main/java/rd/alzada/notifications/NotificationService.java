package rd.alzada.notifications;

import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.TenantContext;
import rd.alzada.config.AppProperties;
import rd.alzada.identity.User;
import rd.alzada.identity.UserRepository;
import rd.alzada.tenants.CompanyRole;
import rd.alzada.tenants.Membership;
import rd.alzada.tenants.MembershipRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notifications;
  private final NotificationTemplateRepository templates;
  private final CompanyNotificationTemplateRepository companyTemplates;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final JavaMailSender mailSender;
  private final AppProperties properties;
  private final NotificationService self;

  public NotificationService(
      NotificationRepository notifications,
      NotificationTemplateRepository templates,
      CompanyNotificationTemplateRepository companyTemplates,
      MembershipRepository memberships,
      UserRepository users,
      JavaMailSender mailSender,
      AppProperties properties,
      @Lazy NotificationService self) {
    this.notifications = notifications;
    this.templates = templates;
    this.companyTemplates = companyTemplates;
    this.memberships = memberships;
    this.users = users;
    this.mailSender = mailSender;
    this.properties = properties;
    this.self = self;
  }

  @Transactional
  public void notifyRole(CompanyRole role, String type, String title, String body, Map<String, String> variables) {
    UUID tenantId = TenantContext.tenantId();
    if (tenantId == null) {
      return;
    }
    Set<UUID> recipients = new LinkedHashSet<>();
    for (Membership membership : memberships.findByCompanyIdAndRoleAndStatus(tenantId, role, Membership.Status.ACTIVE)) {
      recipients.add(membership.getUserId());
    }
    notifyUsers(recipients, type, title, body, variables);
  }

  @Transactional
  public void notifyUsers(Collection<UUID> userIds, String type, String title, String body, Map<String, String> variables) {
    UUID tenantId = TenantContext.tenantId();
    if (tenantId == null) {
      return;
    }
    ResolvedTemplate template = resolve(tenantId, type);
    String subject = TemplateRenderer.render(template.subject(), variables);
    String html = TemplateRenderer.render(template.body(), variables);
    String safeTitle = title == null ? subject : title;
    for (UUID userId : new LinkedHashSet<>(userIds)) {
      User user = users.findById(userId).orElse(null);
      if (user == null || user.getStatus() != User.Status.ACTIVE) {
        continue;
      }
      Notification notification = new Notification();
      notification.setTenantId(tenantId);
      notification.setUserId(userId);
      notification.setType(type);
      notification.setTitle(trim(safeTitle, 180));
      notification.setBody(body == null ? "" : body);
      notification.setEmailStatus(template.enabled() ? Notification.EmailStatus.PENDING : Notification.EmailStatus.SKIPPED);
      notifications.save(notification);
      if (template.enabled()) {
        schedule(notification.getId(), user.getEmail(), subject, html);
      }
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<Notification> inbox(int page, int size) {
    return PageResponse.of(notifications.findByUserIdAndTenantIdOrderByCreatedAtDesc(
        SecuritySupport.currentUserId(),
        SecuritySupport.tenantId(),
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
  }

  @Transactional(readOnly = true)
  public long unread() {
    return notifications.countByUserIdAndTenantIdAndReadAtIsNull(SecuritySupport.currentUserId(), SecuritySupport.tenantId());
  }

  @Transactional
  public void markRead(UUID id) {
    Notification notification = notifications.findByIdAndUserIdAndTenantId(id, SecuritySupport.currentUserId(), SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La notificación no existe"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(java.time.Instant.now());
    }
  }

  @Transactional
  public void markAllRead() {
    notifications.markAllRead(SecuritySupport.currentUserId(), SecuritySupport.tenantId(), java.time.Instant.now());
  }

  @Transactional(readOnly = true)
  public java.util.List<CompanyTemplateView> companyTemplates() {
    java.util.Map<String, CompanyNotificationTemplate> overrides = companyTemplates
        .findByTenantIdOrderByCodeAsc(SecuritySupport.tenantId()).stream()
        .collect(java.util.stream.Collectors.toMap(CompanyNotificationTemplate::getCode, template -> template));
    return templates.findAllByOrderByCodeAsc().stream().map(base -> {
      CompanyNotificationTemplate override = overrides.get(base.getCode());
      if (override == null) {
        return new CompanyTemplateView(base.getCode(), base.getDescription(), base.getSubject(), base.getBody(), true, false);
      }
      return new CompanyTemplateView(base.getCode(), base.getDescription(), override.getSubject(), override.getBody(), override.isEnabled(), true);
    }).toList();
  }

  @Transactional
  public CompanyTemplateView saveCompanyTemplate(String code, String subject, String body, boolean enabled) {
    NotificationTemplate base = templates.findById(code).orElseThrow(() -> ApiException.notFound("La plantilla no existe"));
    if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
      throw ApiException.badRequest("El asunto y el cuerpo son obligatorios");
    }
    if (subject.trim().length() > 180) {
      throw ApiException.badRequest("El asunto es demasiado largo");
    }
    CompanyNotificationTemplate template = companyTemplates.findByTenantIdAndCode(SecuritySupport.tenantId(), code)
        .orElseGet(() -> {
          CompanyNotificationTemplate created = new CompanyNotificationTemplate();
          created.setCode(code);
          return created;
        });
    template.setSubject(subject.trim());
    template.setBody(body);
    template.setEnabled(enabled);
    companyTemplates.save(template);
    return new CompanyTemplateView(base.getCode(), base.getDescription(), template.getSubject(), template.getBody(), template.isEnabled(), true);
  }

  @Transactional
  public void resetCompanyTemplate(String code) {
    companyTemplates.findByTenantIdAndCode(SecuritySupport.tenantId(), code).ifPresent(companyTemplates::delete);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deliver(UUID notificationId, String email, String subject, String html) {
    TenantContext.State previous = TenantContext.state();
    TenantContext.asSystem(previous.userId());
    try {
      Notification notification = notifications.findById(notificationId).orElse(null);
      if (notification == null || notification.getEmailStatus() == Notification.EmailStatus.SENT) {
        return;
      }
      if (!properties.notifications().emailEnabled() || email == null || email.isBlank()) {
        notification.setEmailStatus(Notification.EmailStatus.SKIPPED);
        return;
      }
      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(properties.notifications().from());
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        notification.setEmailStatus(Notification.EmailStatus.SENT);
      } catch (Exception exception) {
        notification.setEmailStatus(Notification.EmailStatus.FAILED);
        log.warn("No se pudo enviar la notificación {}", notificationId);
      }
    } finally {
      restore(previous);
    }
  }

  private void schedule(UUID id, String email, String subject, String html) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      self.deliver(id, email, subject, html);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          self.deliver(id, email, subject, html);
        } catch (RuntimeException exception) {
          log.warn("La notificación {} quedó pendiente de envío", id);
        }
      }
    });
  }

  private ResolvedTemplate resolve(UUID tenantId, String code) {
    return companyTemplates.findByTenantIdAndCode(tenantId, code)
        .map(template -> new ResolvedTemplate(template.getSubject(), template.getBody(), template.isEnabled()))
        .orElseGet(() -> templates.findById(code)
            .map(template -> new ResolvedTemplate(template.getSubject(), template.getBody(), true))
            .orElse(new ResolvedTemplate(code, "<p>{{message}}</p>", true)));
  }

  private static void restore(TenantContext.State state) {
    if (state.bypass()) {
      TenantContext.asSystem(state.userId());
      return;
    }
    if (state.mode() == TenantContext.Mode.MARKETPLACE) {
      TenantContext.asMarketplace(state.userId());
      return;
    }
    if (state.tenantId() != null) {
      TenantContext.asTenant(state.userId(), state.tenantId());
      return;
    }
    TenantContext.clear();
  }

  private static String trim(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }

  private record ResolvedTemplate(String subject, String body, boolean enabled) {}

  public record CompanyTemplateView(
      String code,
      String description,
      String subject,
      String body,
      boolean enabled,
      boolean customized) {}
}
