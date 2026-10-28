package rd.alzada.leads;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.SecurityRoles;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.Texts;
import rd.alzada.config.AppProperties;
import rd.alzada.customers.Customer;
import rd.alzada.customers.CustomerRepository;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.notifications.NotificationService;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.reservations.Reservation;
import rd.alzada.reservations.ReservationRepository;
import rd.alzada.tenants.CompanyRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmService {

  private static final Logger log = LoggerFactory.getLogger(CrmService.class);
  private static final ZoneId SANTO_DOMINGO = ZoneId.of("America/Santo_Domingo");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(SANTO_DOMINGO);

  private final LeadRepository leads;
  private final LeadActivityRepository activities;
  private final LeadPipelineStageRepository stages;
  private final ProjectRepository projects;
  private final UnitRepository units;
  private final ReservationRepository reservations;
  private final CustomerRepository customers;
  private final NotificationService notifications;
  private final AuditService audit;
  private final AppProperties properties;

  @PersistenceContext
  private EntityManager entityManager;

  public CrmService(
      LeadRepository leads,
      LeadActivityRepository activities,
      LeadPipelineStageRepository stages,
      ProjectRepository projects,
      UnitRepository units,
      ReservationRepository reservations,
      CustomerRepository customers,
      NotificationService notifications,
      AuditService audit,
      AppProperties properties) {
    this.leads = leads;
    this.activities = activities;
    this.stages = stages;
    this.projects = projects;
    this.units = units;
    this.reservations = reservations;
    this.customers = customers;
    this.notifications = notifications;
    this.audit = audit;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public List<LeadPipelineStage> pipeline() {
    return stages.findByTenantIdOrderBySortOrderAsc(SecuritySupport.tenantId());
  }

  @Transactional
  public LeadPipelineStage addStage(String name) {
    UUID tenantId = SecuritySupport.tenantId();
    String code = Texts.slug(name).replace('-', '_').toUpperCase();
    if (stages.existsByTenantIdAndCode(tenantId, code)) {
      throw ApiException.conflict("Ya existe una etapa con ese nombre");
    }
    LeadPipelineStage stage = new LeadPipelineStage();
    stage.setTenantId(tenantId);
    stage.setCode(code.length() > 40 ? code.substring(0, 40) : code);
    stage.setName(name.trim());
    stage.setSortOrder(pipeline().size() + 1);
    stage.setClosed(false);
    stage.setSystemDefault(false);
    return stages.save(stage);
  }

  @Transactional
  public LeadPipelineStage renameStage(UUID id, String name, boolean closed) {
    LeadPipelineStage stage = stages.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La etapa no existe"));
    stage.setName(name.trim());
    if (!stage.isSystemDefault()) {
      stage.setClosed(closed);
    }
    return stage;
  }

  @Transactional
  public void deleteStage(UUID id) {
    LeadPipelineStage stage = stages.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La etapa no existe"));
    if (stage.isSystemDefault()) {
      throw ApiException.conflict("Las etapas base no se pueden eliminar");
    }
    boolean used = leads.search(stage.getTenantId(), stage.getCode(), null, null, false, null, Pages.unsorted(0, 1)).hasContent();
    if (used) {
      throw ApiException.conflict("Hay prospectos en esta etapa");
    }
    stages.delete(stage);
  }

  @Transactional(readOnly = true)
  public PageResponse<Lead> searchLeads(String query, String stage, UUID projectId, boolean mine, int page, int size) {
    UUID assigned = null;
    boolean includeUnassigned = false;
    if (!SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      assigned = SecuritySupport.currentUserId();
      includeUnassigned = !mine;
      if (mine) {
        includeUnassigned = false;
      }
    } else if (mine) {
      assigned = SecuritySupport.currentUserId();
    }
    return PageResponse.of(leads.search(SecuritySupport.tenantId(), Texts.blankToNull(stage), projectId, assigned, includeUnassigned, Texts.like(query), Pages.desc(page, size)));
  }

  @Transactional(readOnly = true)
  public LeadDetail lead(UUID id) {
    Lead lead = requireLead(id);
    return new LeadDetail(lead, activities.findByLeadIdAndTenantIdOrderByCreatedAtDesc(id, lead.getTenantId()));
  }

  @Transactional
  public Lead createLead(SaveLeadRequest request, Lead.Source source) {
    UUID tenantId = SecuritySupport.tenantId();
    Lead lead = new Lead();
    lead.setTenantId(tenantId);
    lead.setSource(source);
    lead.setStageCode("NEW");
    applyLead(lead, request);
    if (lead.getAssignedUserId() == null && SecuritySupport.hasRole(SecurityRoles.SALES_ADVISOR) && !SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      lead.setAssignedUserId(SecuritySupport.currentUserId());
    }
    leads.save(lead);
    addActivity(lead, LeadActivity.Type.NOTE, "Prospecto registrado", SecuritySupport.currentUserId());
    notifications.notifyRole(CompanyRole.COMPANY_ADMIN, "NEW_LEAD", "Nuevo prospecto",
        lead.getFullName() + " solicitó información.",
        Map.of(
            "recipientName", "equipo",
            "leadName", lead.getFullName(),
            "leadPhone", lead.getPhone() == null ? "No indicado" : lead.getPhone(),
            "leadEmail", lead.getEmail() == null ? "No indicado" : lead.getEmail(),
            "projectName", projectName(lead.getProjectId())));
    return lead;
  }

  @Transactional
  public Lead createMarketplaceLead(UUID tenantId, UUID projectId, UUID unitId, String name, String email, String phone, String message) {
    Lead lead = new Lead();
    lead.setTenantId(tenantId);
    lead.setFullName(name.trim());
    lead.setEmail(Texts.lowerEmail(email));
    lead.setPhone(Texts.blankToNull(phone));
    lead.setProjectId(projectId);
    lead.setUnitId(unitId);
    lead.setSource(Lead.Source.MARKETPLACE);
    lead.setStageCode("NEW");
    lead.setNotes(Texts.blankToNull(message));
    leads.save(lead);
    addActivity(lead, LeadActivity.Type.NOTE, "Solicitud recibida desde el marketplace", null);
    notifications.notifyRole(CompanyRole.COMPANY_ADMIN, "NEW_LEAD", "Nuevo prospecto del marketplace",
        lead.getFullName() + " escribió desde el marketplace.",
        Map.of(
            "recipientName", "equipo",
            "leadName", lead.getFullName(),
            "leadPhone", lead.getPhone() == null ? "No indicado" : lead.getPhone(),
            "leadEmail", lead.getEmail() == null ? "No indicado" : lead.getEmail(),
            "projectName", projectName(projectId)));
    return lead;
  }

  @Transactional
  public Lead updateLead(UUID id, SaveLeadRequest request) {
    Lead lead = requireLead(id);
    assertCanTouch(lead);
    String previous = lead.getStageCode();
    applyLead(lead, request);
    if (!previous.equals(lead.getStageCode())) {
      addActivity(lead, LeadActivity.Type.STATUS_CHANGE, "Cambió la etapa a " + lead.getStageCode(), SecuritySupport.currentUserId());
    }
    return lead;
  }

  @Transactional
  public LeadActivity addNote(UUID id, LeadActivity.Type type, String description) {
    Lead lead = requireLead(id);
    assertCanTouch(lead);
    return addActivity(lead, type == null ? LeadActivity.Type.NOTE : type, description, SecuritySupport.currentUserId());
  }

  @Transactional(readOnly = true)
  public PageResponse<Reservation> searchReservations(Reservation.Status status, int page, int size) {
    return PageResponse.of(reservations.search(SecuritySupport.tenantId(), status, null, Pages.desc(page, size)));
  }

  @Transactional
  public Reservation reserve(UUID unitId, UUID leadId, UUID customerId, BigDecimal amount, String currency, Instant expiresAt, String notes) {
    if (leadId == null && customerId == null) {
      throw ApiException.badRequest("Indique el prospecto o el comprador de la reserva");
    }
    UUID tenantId = SecuritySupport.tenantId();
    Unit unit = units.lock(unitId, tenantId).orElseThrow(() -> ApiException.notFound("La unidad no existe"));
    if (unit.getAvailability() != Unit.Availability.AVAILABLE) {
      throw ApiException.conflict("La unidad no está disponible");
    }
    Lead lead = null;
    if (leadId != null) {
      lead = requireLead(leadId);
      assertCanTouch(lead);
    }
    if (customerId != null) {
      customers.findByIdAndTenantId(customerId, tenantId).orElseThrow(() -> ApiException.notFound("El comprador no existe"));
    }
    Instant expiry = expiresAt == null ? Instant.now().plusSeconds(properties.reservations().defaultHours() * 3600L) : expiresAt;
    if (!expiry.isAfter(Instant.now()) || expiry.isAfter(Instant.now().plusSeconds(365L * 24 * 3600))) {
      throw ApiException.badRequest("La fecha de vencimiento de la reserva no es válida");
    }
    Reservation reservation = new Reservation();
    reservation.setTenantId(tenantId);
    reservation.setUnitId(unitId);
    reservation.setLeadId(leadId);
    reservation.setCustomerId(customerId);
    reservation.setAmount(amount);
    reservation.setCurrency(currency == null || currency.isBlank() ? "DOP" : currency.trim().toUpperCase());
    reservation.setExpiresAt(expiry);
    reservation.setStatus(Reservation.Status.ACTIVE);
    reservation.setResponsibleUserId(SecuritySupport.currentUserId());
    reservation.setNotes(Texts.blankToNull(notes));
    reservations.save(reservation);
    unit.setAvailability(Unit.Availability.RESERVED);
    if (lead != null && "NEW".equals(lead.getStageCode())) {
      lead.setStageCode("RESERVED");
    }
    notifications.notifyRole(CompanyRole.COMPANY_ADMIN, "NEW_RESERVATION", "Nueva reserva",
        "Se reservó la unidad " + unit.getCode() + " hasta " + DATE_TIME.format(expiry) + ".",
        Map.of("projectName", projectName(unit.getProjectId()), "unitCode", unit.getCode(), "expiresAt", DATE_TIME.format(expiry)));
    audit.record("RESERVATION_CREATED", "RESERVATION", reservation.getId(), "Reservó la unidad " + unit.getCode(), Map.of());
    return reservation;
  }

  @Transactional
  public Reservation cancel(UUID id, String notes) {
    Reservation reservation = reservations.lock(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La reserva no existe"));
    if (reservation.getStatus() != Reservation.Status.ACTIVE) {
      throw ApiException.conflict("Solo se puede cancelar una reserva activa");
    }
    if (!SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)
        && reservation.getResponsibleUserId() != null
        && !reservation.getResponsibleUserId().equals(SecuritySupport.currentUserId())) {
      throw ApiException.forbidden("Esta reserva pertenece a otro asesor");
    }
    release(reservation, Reservation.Status.CANCELLED, notes);
    audit.record("RESERVATION_CANCELLED", "RESERVATION", reservation.getId(), "Canceló una reserva", Map.of());
    return reservation;
  }

  @Transactional
  public List<Reservation> expiredReservations() {
    if (!lockJob(8421001)) {
      return List.of();
    }
    return reservations.findExpired(Instant.now());
  }

  @Transactional
  public List<Reservation> expiringReservations() {
    if (!lockJob(8421002)) {
      return List.of();
    }
    Instant now = Instant.now();
    return reservations.findExpiring(now, now.plusSeconds(properties.reservations().expiringNoticeHours() * 3600L));
  }

  @Transactional
  public void expireOne(UUID id, UUID tenantId) {
    Reservation reservation = reservations.lock(id, tenantId).orElse(null);
    if (reservation == null || reservation.getStatus() != Reservation.Status.ACTIVE || reservation.getExpiresAt().isAfter(Instant.now())) {
      return;
    }
    release(reservation, Reservation.Status.EXPIRED, "La reserva venció automáticamente");
  }

  @Transactional
  public void noticeExpiring(UUID id, UUID tenantId) {
    Reservation reservation = reservations.findByIdAndTenantId(id, tenantId).orElse(null);
    if (reservation == null || reservation.getExpiryNoticeSentAt() != null || reservation.getStatus() != Reservation.Status.ACTIVE) {
      return;
    }
    Unit unit = units.findByIdAndTenantId(reservation.getUnitId(), tenantId).orElse(null);
    reservation.setExpiryNoticeSentAt(Instant.now());
    if (reservation.getResponsibleUserId() != null) {
      notifications.notifyUsers(List.of(reservation.getResponsibleUserId()), "RESERVATION_EXPIRING",
          "Reserva por vencer",
          "La reserva de la unidad " + (unit == null ? "" : unit.getCode()) + " vence pronto.",
          Map.of(
              "unitCode", unit == null ? "" : unit.getCode(),
              "expiresAt", DATE_TIME.format(reservation.getExpiresAt()),
              "companyName", "su constructora",
              "projectName", unit == null ? "" : projectName(unit.getProjectId())));
    }
  }

  private void release(Reservation reservation, Reservation.Status status, String notes) {
    reservation.setStatus(status);
    if (notes != null && !notes.isBlank()) {
      reservation.setNotes(notes.trim());
    }
    Unit unit = units.lock(reservation.getUnitId(), reservation.getTenantId()).orElse(null);
    if (unit != null
        && unit.getAvailability() == Unit.Availability.RESERVED
        && reservations.findByUnitIdAndStatus(unit.getId(), Reservation.Status.ACTIVE).isEmpty()) {
      unit.setAvailability(Unit.Availability.AVAILABLE);
    }
  }

  private Lead requireLead(UUID id) {
    return leads.findByIdAndTenantId(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("El prospecto no existe"));
  }

  private void assertCanTouch(Lead lead) {
    if (SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      return;
    }
    if (lead.getAssignedUserId() != null && !lead.getAssignedUserId().equals(SecuritySupport.currentUserId())) {
      throw ApiException.forbidden("Este prospecto está asignado a otro asesor");
    }
  }

  private void applyLead(Lead lead, SaveLeadRequest request) {
    UUID tenantId = lead.getTenantId();
    if (request.projectId() != null) {
      projects.findByIdAndTenantId(request.projectId(), tenantId).orElseThrow(() -> ApiException.badRequest("El proyecto no pertenece a la constructora"));
    }
    if (request.unitId() != null) {
      Unit unit = units.findByIdAndTenantId(request.unitId(), tenantId).orElseThrow(() -> ApiException.badRequest("La unidad no pertenece a la constructora"));
      if (request.projectId() != null && !unit.getProjectId().equals(request.projectId())) {
        throw ApiException.badRequest("La unidad no pertenece al proyecto indicado");
      }
    }
    if (request.stageCode() != null) {
      stages.findByTenantIdAndCode(tenantId, request.stageCode()).orElseThrow(() -> ApiException.badRequest("La etapa comercial no existe"));
      lead.setStageCode(request.stageCode());
    }
    lead.setFullName(request.fullName().trim());
    lead.setEmail(Texts.lowerEmail(request.email()));
    lead.setPhone(Texts.blankToNull(request.phone()));
    lead.setProjectId(request.projectId());
    lead.setUnitId(request.unitId());
    lead.setNotes(Texts.blankToNull(request.notes()));
    lead.setNextStep(Texts.blankToNull(request.nextStep()));
    lead.setNextStepDate(request.nextStepDate());
    if (SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      lead.setAssignedUserId(request.assignedUserId());
    }
  }

  private LeadActivity addActivity(Lead lead, LeadActivity.Type type, String description, UUID actor) {
    if (description == null || description.isBlank()) {
      throw ApiException.badRequest("Escriba el detalle de la actividad");
    }
    LeadActivity activity = new LeadActivity();
    activity.setTenantId(lead.getTenantId());
    activity.setLeadId(lead.getId());
    activity.setType(type);
    activity.setDescription(description.trim());
    activity.setCreatedBy(actor);
    return activities.save(activity);
  }

  private String projectName(UUID projectId) {
    if (projectId == null) {
      return "un proyecto";
    }
    return projects.findById(projectId).map(project -> project.getName()).orElse("un proyecto");
  }

  private boolean lockJob(int key) {
    Object result = entityManager.createNativeQuery("select pg_try_advisory_xact_lock(" + key + ")").getSingleResult();
    boolean locked = Boolean.TRUE.equals(result);
    if (!locked) {
      log.debug("Otro proceso ya ejecuta el mantenimiento {}", key);
    }
    return locked;
  }

  public record SaveLeadRequest(
      String fullName,
      String email,
      String phone,
      UUID projectId,
      UUID unitId,
      UUID assignedUserId,
      String stageCode,
      String notes,
      String nextStep,
      LocalDate nextStepDate) {}

  public record LeadDetail(Lead lead, List<LeadActivity> activities) {}
}
