package rd.alzada.notifications;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_notification_templates")
@Getter
@Setter
public class CompanyNotificationTemplate extends TenantEntity {

  @Column(nullable = false, length = 60)
  private String code;

  @Column(nullable = false, length = 180)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(nullable = false)
  private boolean enabled = true;
}
