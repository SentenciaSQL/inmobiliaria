package rd.alzada.notifications;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyNotificationTemplateRepository extends JpaRepository<CompanyNotificationTemplate, UUID> {

  List<CompanyNotificationTemplate> findByTenantIdOrderByCodeAsc(UUID tenantId);

  Optional<CompanyNotificationTemplate> findByTenantIdAndCode(UUID tenantId, String code);
}
