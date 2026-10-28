package rd.alzada.notifications;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

  List<NotificationTemplate> findAllByOrderByCodeAsc();
}
