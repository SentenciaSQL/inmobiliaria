package rd.alzada.audit;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

  private final AuditService audit;

  public AuditController(AuditService audit) {
    this.audit = audit;
  }

  @GetMapping
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public PageResponse<AuditEvent> company(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return audit.company(page, size);
  }
}
