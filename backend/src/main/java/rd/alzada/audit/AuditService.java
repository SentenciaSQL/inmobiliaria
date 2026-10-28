package rd.alzada.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import rd.alzada.common.PageResponse;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditEventRepository events;
  private final ObjectMapper objectMapper;

  public AuditService(AuditEventRepository events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void record(String action, String entityType, UUID entityId, String summary, Map<String, ?> details) {
    AuditEvent event = new AuditEvent();
    event.setTenantId(TenantContext.tenantId());
    event.setActorId(TenantContext.userId());
    event.setAction(action);
    event.setEntityType(entityType);
    event.setEntityId(entityId);
    event.setSummary(summary.length() > 400 ? summary.substring(0, 400) : summary);
    if (details != null && !details.isEmpty()) {
      try {
        event.setDetails(objectMapper.writeValueAsString(details));
      } catch (JsonProcessingException exception) {
        event.setDetails(null);
      }
    }
    events.save(event);
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditEvent> company(int page, int size) {
    return PageResponse.of(events.findByTenantIdOrderByCreatedAtDesc(
        SecuritySupport.tenantId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditEvent> platform(int page, int size) {
    return PageResponse.of(events.platform(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
  }
}
