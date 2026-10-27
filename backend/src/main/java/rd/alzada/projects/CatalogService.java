package rd.alzada.projects;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.Texts;
import rd.alzada.config.AppProperties;
import rd.alzada.contracts.Contract;
import rd.alzada.contracts.ContractPartyRepository;
import rd.alzada.contracts.ContractRepository;
import rd.alzada.customers.CustomerRepository;
import rd.alzada.inventory.Building;
import rd.alzada.inventory.BuildingRepository;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.notifications.NotificationService;
import rd.alzada.reservations.ReservationRepository;
import rd.alzada.storage.FileRules;
import rd.alzada.storage.ObjectStorage;
import rd.alzada.storage.StoredFile;
import rd.alzada.tenants.CompanyRole;
import rd.alzada.tenants.LocationCatalog;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {

  private final ProjectRepository projects;
  private final ProjectImageRepository images;
  private final BuildingRepository buildings;
  private final UnitRepository units;
  private final ContractRepository contracts;
  private final ContractPartyRepository parties;
  private final CustomerRepository customers;
  private final ReservationRepository reservations;
  private final LocationCatalog locations;
  private final ObjectStorage storage;
  private final AppProperties properties;
  private final AuditService audit;
  private final NotificationService notifications;

  public CatalogService(
      ProjectRepository projects,
      ProjectImageRepository images,
      BuildingRepository buildings,
      UnitRepository units,
      ContractRepository contracts,
      ContractPartyRepository parties,
      CustomerRepository customers,
      ReservationRepository reservations,
      LocationCatalog locations,
      ObjectStorage storage,
      AppProperties properties,
      AuditService audit,
      NotificationService notifications) {
    this.projects = projects;
    this.images = images;
    this.buildings = buildings;
    this.units = units;
    this.contracts = contracts;
    this.parties = parties;
    this.customers = customers;
    this.reservations = reservations;
    this.locations = locations;
    this.storage = storage;
    this.properties = properties;
    this.audit = audit;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public PageResponse<ProjectResponse> search(String query, Project.ProjectStatus status, Project.PublicationStatus publication, int page, int size) {
    return PageResponse.of(projects.search(SecuritySupport.tenantId(), Texts.like(query), status, publication, Pages.desc(page, size))
        .map(this::toSummary));
  }

  @Transactional(readOnly = true)
  public ProjectResponse get(UUID id) {
    return toDetail(requireProject(id));
  }

  @Transactional
  public ProjectResponse create(SaveProjectRequest request) {
    Project project = new Project();
    project.setTenantId(SecuritySupport.tenantId());
    project.setPublicationStatus(Project.PublicationStatus.DRAFT);
    apply(project, request);
    project.setSlug(uniqueSlug(Texts.slug(request.name())));
    projects.save(project);
    audit.record("PROJECT_CREATED", "PROJECT", project.getId(), "Creó el proyecto " + project.getName(), Map.of());
    return toDetail(project);
  }

  @Transactional
  public ProjectResponse update(UUID id, SaveProjectRequest request) {
    Project project = requireProject(id);
    apply(project, request);
    audit.record("PROJECT_UPDATED", "PROJECT", project.getId(), "Actualizó el proyecto " + project.getName(), Map.of());
    return toDetail(project);
  }

  @Transactional
  public void delete(UUID id) {
    Project project = requireProject(id);
    boolean blocked = units.findByProjectIdAndTenantIdOrderByCodeAsc(id, project.getTenantId()).stream()
        .anyMatch(unit -> unit.getAvailability() == Unit.Availability.RESERVED || unit.getAvailability() == Unit.Availability.SOLD);
    if (blocked) {
      throw ApiException.conflict("No se puede eliminar un proyecto con unidades reservadas o vendidas. Ocultelo del marketplace");
    }
    project.setDeletedAt(Instant.now());
    project.setPublicationStatus(Project.PublicationStatus.HIDDEN);
    project.setFeatured(false);
    audit.record("PROJECT_DELETED", "PROJECT", project.getId(), "Eliminó lógicamente el proyecto " + project.getName(), Map.of());
  }

  @Transactional
  public ProjectResponse publish(UUID id, Project.PublicationStatus publication, boolean featured) {
    Project project = requireProject(id);
    if (featured && publication != Project.PublicationStatus.PUBLISHED) {
      throw ApiException.badRequest("Solo un proyecto publicado puede destacarse");
    }
    boolean becamePublic = publication == Project.PublicationStatus.PUBLISHED && project.getPublicationStatus() != Project.PublicationStatus.PUBLISHED;
    project.setPublicationStatus(publication);
    project.setFeatured(publication == Project.PublicationStatus.PUBLISHED && featured);
    if (becamePublic) {
      project.setPublishedAt(Instant.now());
      notifications.notifyRole(CompanyRole.COMPANY_ADMIN, "PROJECT_PUBLISHED",
          "Proyecto publicado",
          "El proyecto " + project.getName() + " ya es visible en el marketplace.",
          Map.of("projectName", project.getName(), "companyName", "su constructora"));
      audit.record("PROJECT_PUBLISHED", "PROJECT", project.getId(), "Publicó el proyecto " + project.getName(), Map.of());
    }
    return toDetail(project);
  }

  @Transactional
  public void notifyBuyers(UUID id, String message) {
    Project project = requireProject(id);
    if (message == null || message.isBlank()) {
      throw ApiException.badRequest("Escriba el mensaje para los compradores");
    }
    Set<UUID> recipients = new LinkedHashSet<>();
    for (Contract contract : contracts.findByProjectIdAndTenantIdAndStatus(id, project.getTenantId(), Contract.Status.ACTIVE)) {
      parties.findByContractIdAndTenantId(contract.getId(), project.getTenantId()).forEach(party ->
          customers.findByIdAndTenantId(party.getCustomerId(), project.getTenantId())
              .filter(customer -> customer.getUserId() != null)
              .ifPresent(customer -> recipients.add(customer.getUserId())));
    }
    notifications.notifyUsers(recipients, "PROJECT_UPDATE", "Actualización de " + project.getName(), message.trim(),
        Map.of("projectName", project.getName(), "companyName", "su constructora", "message", message.trim(), "recipientName", "comprador"));
  }

  @Transactional
  public ProjectImage addImage(UUID projectId, MultipartFile file, String alt) {
    Project project = requireProject(projectId);
    if (images.countByProjectId(projectId) >= 12) {
      throw ApiException.badRequest("El proyecto admite hasta 12 fotografías");
    }
    StoredFile image = FileRules.image(FileRules.read(file, properties.uploads().imageMaxBytes()));
    String key = "companies/" + project.getTenantId() + "/projects/" + projectId + "/" + UUID.randomUUID() + "." + image.extension();
    storage.put(key, image.content(), image.contentType());
    ProjectImage entity = new ProjectImage();
    entity.setTenantId(project.getTenantId());
    entity.setProjectId(projectId);
    entity.setObjectKey(key);
    entity.setContentType(image.contentType());
    entity.setSortOrder(images.maxSortOrder(projectId) + 1);
    entity.setAltText(Texts.blankToNull(alt));
    images.save(entity);
    rememberRollback(key);
    return entity;
  }

  @Transactional
  public void deleteImage(UUID projectId, UUID imageId) {
    ProjectImage image = images.findByIdAndProjectIdAndTenantId(imageId, projectId, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La fotografía no existe"));
    String key = image.getObjectKey();
    images.delete(image);
    rememberDelete(key);
  }

  @Transactional(readOnly = true)
  public ProjectImage requireImage(UUID projectId, UUID imageId) {
    return images.findByIdAndProjectIdAndTenantId(imageId, projectId, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La fotografía no existe"));
  }

  @Transactional(readOnly = true)
  public FileContent imageContent(UUID projectId, UUID imageId) {
    ProjectImage image = requireImage(projectId, imageId);
    return new FileContent(storage.get(image.getObjectKey()), image.getContentType());
  }

  @Transactional(readOnly = true)
  public List<Building> buildings(UUID projectId) {
    requireProject(projectId);
    return buildings.findByProjectIdAndTenantIdOrderByNameAsc(projectId, SecuritySupport.tenantId());
  }

  @Transactional
  public Building createBuilding(UUID projectId, String name, String code, Integer floors) {
    requireProject(projectId);
    if (buildings.existsByProjectIdAndCodeIgnoreCase(projectId, code.trim())) {
      throw ApiException.conflict("Ya existe un edificio con ese código");
    }
    Building building = new Building();
    building.setTenantId(SecuritySupport.tenantId());
    building.setProjectId(projectId);
    building.setName(name.trim());
    building.setCode(code.trim());
    building.setFloors(floors);
    return buildings.save(building);
  }

  @Transactional
  public Building updateBuilding(UUID id, String name, String code, Integer floors) {
    Building building = buildings.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("El edificio no existe"));
    building.setName(name.trim());
    building.setCode(code.trim());
    building.setFloors(floors);
    return building;
  }

  @Transactional
  public void deleteBuilding(UUID id) {
    Building building = buildings.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("El edificio no existe"));
    if (units.countByBuildingId(id) > 0) {
      throw ApiException.conflict("El edificio tiene unidades asociadas");
    }
    buildings.delete(building);
  }

  @Transactional(readOnly = true)
  public PageResponse<Unit> searchUnits(UUID projectId, Unit.Availability availability, String query, int page, int size) {
    return PageResponse.of(units.search(SecuritySupport.tenantId(), projectId, availability, Texts.like(query), Pages.desc(page, size)));
  }

  @Transactional(readOnly = true)
  public List<Unit> unitsOf(UUID projectId) {
    requireProject(projectId);
    return units.findByProjectIdAndTenantIdOrderByCodeAsc(projectId, SecuritySupport.tenantId());
  }

  @Transactional
  public Unit createUnit(UUID projectId, SaveUnitRequest request) {
    requireProject(projectId);
    if (request.buildingId() != null && buildings.countByIdAndProjectId(request.buildingId(), projectId) == 0) {
      throw ApiException.badRequest("El edificio no pertenece al proyecto");
    }
    if (units.existsByProjectIdAndCodeIgnoreCase(projectId, request.code().trim())) {
      throw ApiException.conflict("Ya existe una unidad con ese código");
    }
    Unit unit = new Unit();
    unit.setTenantId(SecuritySupport.tenantId());
    unit.setProjectId(projectId);
    unit.setAvailability(Unit.Availability.AVAILABLE);
    applyUnit(unit, request);
    return units.save(unit);
  }

  @Transactional
  public Unit updateUnit(UUID id, SaveUnitRequest request) {
    Unit unit = requireUnit(id);
    if (request.buildingId() != null && buildings.countByIdAndProjectId(request.buildingId(), unit.getProjectId()) == 0) {
      throw ApiException.badRequest("El edificio no pertenece al proyecto");
    }
    applyUnit(unit, request);
    return unit;
  }

  @Transactional
  public Unit changeAvailability(UUID id, Unit.Availability target) {
    Unit unit = units.lock(id, SecuritySupport.tenantId()).orElseThrow(() -> ApiException.notFound("La unidad no existe"));
    if (target == Unit.Availability.RESERVED || target == Unit.Availability.SOLD) {
      throw ApiException.badRequest("Reserve la unidad o active un contrato para cambiar a ese estado");
    }
    if (target == Unit.Availability.AVAILABLE) {
      if (unit.getAvailability() == Unit.Availability.SOLD) {
        throw ApiException.conflict("Una unidad vendida solo vuelve al mercado si se anula el contrato");
      }
      if (reservations.findByUnitIdAndStatus(unit.getId(), rd.alzada.reservations.Reservation.Status.ACTIVE).isPresent()) {
        throw ApiException.conflict("La unidad tiene una reserva activa");
      }
      if (contracts.existsByUnitIdAndStatusIn(unit.getId(), List.of(Contract.Status.ACTIVE, Contract.Status.DRAFT))) {
        throw ApiException.conflict("La unidad tiene un contrato abierto");
      }
    }
    if (target == Unit.Availability.OFF_MARKET && (unit.getAvailability() == Unit.Availability.RESERVED || unit.getAvailability() == Unit.Availability.SOLD)) {
      throw ApiException.conflict("No se puede retirar del mercado una unidad reservada o vendida");
    }
    unit.setAvailability(target);
    audit.record("UNIT_STATUS", "UNIT", unit.getId(), "Cambió la disponibilidad de la unidad " + unit.getCode(), Map.of("availability", target.name()));
    return unit;
  }

  @Transactional
  public void deleteUnit(UUID id) {
    Unit unit = requireUnit(id);
    if (unit.getAvailability() == Unit.Availability.RESERVED || unit.getAvailability() == Unit.Availability.SOLD) {
      throw ApiException.conflict("No se puede eliminar una unidad reservada o vendida");
    }
    unit.setDeletedAt(Instant.now());
  }

  private void apply(Project project, SaveProjectRequest request) {
    locations.require(request.provinceCode(), request.municipalityId());
    if (request.latitude() != null && (request.latitude().abs().compareTo(new BigDecimal("90")) > 0
        || request.longitude() == null || request.longitude().abs().compareTo(new BigDecimal("180")) > 0)) {
      throw ApiException.badRequest("Las coordenadas no son válidas");
    }
    project.setName(request.name().trim());
    project.setDescription(Texts.blankToNull(request.description()));
    project.setDevelopmentType(request.developmentType());
    project.setProvinceCode(request.provinceCode());
    project.setMunicipalityId(request.municipalityId());
    project.setAddress(Texts.blankToNull(request.address()));
    project.setLatitude(request.latitude());
    project.setLongitude(request.longitude());
    project.setEstimatedStart(request.estimatedStart());
    project.setEstimatedDelivery(request.estimatedDelivery());
    project.setStatus(request.status());
    project.setAmenities(cleanList(request.amenities()));
    project.setFeatures(cleanList(request.features()));
    project.setCommercialInfo(Texts.blankToNull(request.commercialInfo()));
    project.setInternalNotes(Texts.blankToNull(request.internalNotes()));
    project.setMinPrice(request.minPrice());
    project.setCurrency(currency(request.currency()));
    project.setSearchText(Texts.truncate(Texts.fold(project.getName() + " " + locations.provinceName(project.getProvinceCode()) + " " + locations.municipalityName(project.getMunicipalityId())), 500));
  }

  private void applyUnit(Unit unit, SaveUnitRequest request) {
    unit.setBuildingId(request.buildingId());
    unit.setCode(request.code().trim());
    unit.setPropertyType(request.propertyType());
    unit.setBedrooms(request.bedrooms());
    unit.setBathrooms(request.bathrooms());
    unit.setAreaM2(request.areaM2());
    unit.setLevelLabel(Texts.blankToNull(request.levelLabel()));
    if (request.price() == null || request.price().signum() < 0) {
      throw ApiException.badRequest("Indique un precio válido");
    }
    unit.setPrice(request.price());
    unit.setCurrency(currency(request.currency()));
    unit.setFeatures(cleanList(request.features()));
    unit.setPubliclyVisible(request.publiclyVisible());
  }

  private Project requireProject(UUID id) {
    return projects.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("El proyecto no existe"));
  }

  private Unit requireUnit(UUID id) {
    return units.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("La unidad no existe"));
  }

  private String uniqueSlug(String base) {
    String candidate = base;
    int suffix = 2;
    while (projects.existsBySlug(candidate)) {
      candidate = Texts.truncate(base, 140) + "-" + suffix++;
    }
    return candidate;
  }

  private ProjectResponse toSummary(Project project) {
    return toDetail(project);
  }

  private ProjectResponse toDetail(Project project) {
    List<ProjectImage> gallery = images.findByProjectIdOrderBySortOrderAsc(project.getId());
    return new ProjectResponse(
        project.getId(), project.getName(), project.getSlug(), project.getDescription(), project.getDevelopmentType(),
        project.getProvinceCode(), locations.provinceName(project.getProvinceCode()), project.getMunicipalityId(),
        locations.municipalityName(project.getMunicipalityId()), project.getAddress(), project.getLatitude(),
        project.getLongitude(), project.getEstimatedStart(), project.getEstimatedDelivery(), project.getStatus(),
        project.getPublicationStatus(), project.isFeatured(), project.getAmenities(), project.getFeatures(),
        project.getCommercialInfo(), project.getInternalNotes(), project.getMinPrice(), project.getCurrency(),
        gallery.stream().map(image -> new ImageResponse(image.getId(), image.getAltText(), image.getSortOrder(),
            "/api/v1/projects/" + project.getId() + "/images/" + image.getId() + "/content")).toList());
  }

  private static List<String> cleanList(List<String> values) {
    if (values == null) {
      return new ArrayList<>();
    }
    return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().limit(40).toList();
  }

  private static String currency(String value) {
    String currency = value == null || value.isBlank() ? "DOP" : value.trim().toUpperCase();
    if (!currency.matches("^[A-Z]{3}$")) {
      throw ApiException.badRequest("La moneda debe ser un código de tres letras");
    }
    return currency;
  }

  private void rememberRollback(String key) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) {
          storage.delete(key);
        }
      }
    });
  }

  private void rememberDelete(String key) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        storage.delete(key);
      }
    });
  }

  public record SaveProjectRequest(
      String name,
      String description,
      Project.DevelopmentType developmentType,
      String provinceCode,
      UUID municipalityId,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      LocalDate estimatedStart,
      LocalDate estimatedDelivery,
      Project.ProjectStatus status,
      List<String> amenities,
      List<String> features,
      String commercialInfo,
      String internalNotes,
      BigDecimal minPrice,
      String currency) {}

  public record SaveUnitRequest(
      UUID buildingId,
      String code,
      Unit.PropertyType propertyType,
      Integer bedrooms,
      BigDecimal bathrooms,
      BigDecimal areaM2,
      String levelLabel,
      BigDecimal price,
      String currency,
      List<String> features,
      boolean publiclyVisible) {}

  public record ProjectResponse(
      UUID id,
      String name,
      String slug,
      String description,
      Project.DevelopmentType developmentType,
      String provinceCode,
      String provinceName,
      UUID municipalityId,
      String municipalityName,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      LocalDate estimatedStart,
      LocalDate estimatedDelivery,
      Project.ProjectStatus status,
      Project.PublicationStatus publicationStatus,
      boolean featured,
      List<String> amenities,
      List<String> features,
      String commercialInfo,
      String internalNotes,
      BigDecimal minPrice,
      String currency,
      List<ImageResponse> images) {}

  public record ImageResponse(UUID id, String altText, int sortOrder, String url) {}

  public record FileContent(byte[] content, String contentType) {}
}
