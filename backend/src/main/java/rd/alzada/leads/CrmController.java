package rd.alzada.leads;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import rd.alzada.reservations.Reservation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
public class CrmController {

  private final CrmService crm;

  public CrmController(CrmService crm) {
    this.crm = crm;
  }

  @GetMapping("/pipeline")
  @PreAuthorize(SecurityRoles.SALES)
  public List<LeadPipelineStage> pipeline() {
    return crm.pipeline();
  }

  @PostMapping("/pipeline")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public LeadPipelineStage addStage(@Valid @RequestBody StageBody body) {
    return crm.addStage(body.name());
  }

  @PatchMapping("/pipeline/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public LeadPipelineStage renameStage(@PathVariable UUID id, @Valid @RequestBody StageUpdate body) {
    return crm.renameStage(id, body.name(), body.closed());
  }

  @DeleteMapping("/pipeline/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void deleteStage(@PathVariable UUID id) {
    crm.deleteStage(id);
  }

  @GetMapping("/leads")
  @PreAuthorize(SecurityRoles.SALES)
  public PageResponse<Lead> leads(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "false") boolean mine,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return crm.searchLeads(q, stage, projectId, mine, page, size);
  }

  @GetMapping("/leads/{id}")
  @PreAuthorize(SecurityRoles.SALES)
  public CrmService.LeadDetail lead(@PathVariable UUID id) {
    return crm.lead(id);
  }

  @PostMapping("/leads")
  @PreAuthorize(SecurityRoles.SALES)
  public Lead createLead(@Valid @RequestBody LeadBody body) {
    return crm.createLead(body.toCommand(), body.source() == null ? Lead.Source.MANUAL : body.source());
  }

  @PutMapping("/leads/{id}")
  @PreAuthorize(SecurityRoles.SALES)
  public Lead updateLead(@PathVariable UUID id, @Valid @RequestBody LeadBody body) {
    return crm.updateLead(id, body.toCommand());
  }

  @PostMapping("/leads/{id}/activities")
  @PreAuthorize(SecurityRoles.SALES)
  public LeadActivity addActivity(@PathVariable UUID id, @Valid @RequestBody ActivityBody body) {
    return crm.addNote(id, body.type(), body.description());
  }

  @GetMapping("/reservations")
  @PreAuthorize(SecurityRoles.SALES)
  public PageResponse<Reservation> reservations(
      @RequestParam(required = false) Reservation.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return crm.searchReservations(status, page, size);
  }

  @PostMapping("/reservations")
  @PreAuthorize(SecurityRoles.SALES)
  public Reservation reserve(@Valid @RequestBody ReservationBody body) {
    return crm.reserve(body.unitId(), body.leadId(), body.customerId(), body.amount(), body.currency(), body.expiresAt(), body.notes());
  }

  @PostMapping("/reservations/{id}/cancel")
  @PreAuthorize(SecurityRoles.SALES)
  public Reservation cancel(@PathVariable UUID id, @RequestBody(required = false) CancelBody body) {
    return crm.cancel(id, body == null ? null : body.notes());
  }

  public record StageBody(@NotBlank @Size(max = 80) String name) {}

  public record StageUpdate(@NotBlank @Size(max = 80) String name, boolean closed) {}

  public record LeadBody(
      @NotBlank @Size(max = 160) String fullName,
      @Email @Size(max = 180) String email,
      @Size(max = 30) String phone,
      UUID projectId,
      UUID unitId,
      UUID assignedUserId,
      @Size(max = 40) String stageCode,
      @Size(max = 4000) String notes,
      @Size(max = 240) String nextStep,
      LocalDate nextStepDate,
      Lead.Source source) {

    CrmService.SaveLeadRequest toCommand() {
      return new CrmService.SaveLeadRequest(fullName, email, phone, projectId, unitId, assignedUserId, stageCode, notes, nextStep, nextStepDate);
    }
  }

  public record ActivityBody(@NotNull LeadActivity.Type type, @NotBlank @Size(max = 2000) String description) {}

  public record ReservationBody(
      @NotNull UUID unitId,
      UUID leadId,
      UUID customerId,
      BigDecimal amount,
      @Size(max = 3) String currency,
      Instant expiresAt,
      @Size(max = 2000) String notes) {}

  public record CancelBody(@Size(max = 2000) String notes) {}
}
