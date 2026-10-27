package rd.alzada.projects;

import rd.alzada.common.HttpFiles;
import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import rd.alzada.inventory.Building;
import rd.alzada.inventory.Unit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/projects")
  @PreAuthorize(SecurityRoles.STAFF)
  public PageResponse<CatalogService.ProjectResponse> projects(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Project.ProjectStatus status,
      @RequestParam(required = false) Project.PublicationStatus publication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return catalog.search(q, status, publication, page, size);
  }

  @PostMapping("/projects")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CatalogService.ProjectResponse createProject(@Valid @RequestBody ProjectBody body) {
    return catalog.create(body.toCommand());
  }

  @GetMapping("/projects/{id}")
  @PreAuthorize(SecurityRoles.STAFF)
  public CatalogService.ProjectResponse project(@PathVariable UUID id) {
    return catalog.get(id);
  }

  @PutMapping("/projects/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CatalogService.ProjectResponse updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectBody body) {
    return catalog.update(id, body.toCommand());
  }

  @DeleteMapping("/projects/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void deleteProject(@PathVariable UUID id) {
    catalog.delete(id);
  }

  @PatchMapping("/projects/{id}/publication")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CatalogService.ProjectResponse publish(@PathVariable UUID id, @Valid @RequestBody PublicationBody body) {
    return catalog.publish(id, body.publication(), body.featured());
  }

  @PostMapping("/projects/{id}/notify")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void notifyBuyers(@PathVariable UUID id, @Valid @RequestBody NoticeBody body) {
    catalog.notifyBuyers(id, body.message());
  }

  @PostMapping(value = "/projects/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CatalogService.ImageResponse addImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file, @RequestParam(required = false) String alt) {
    ProjectImage image = catalog.addImage(id, file, alt);
    return new CatalogService.ImageResponse(
        image.getId(),
        image.getAltText(),
        image.getSortOrder(),
        "/api/v1/projects/" + id + "/images/" + image.getId() + "/content");
  }

  @DeleteMapping("/projects/{id}/images/{imageId}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
    catalog.deleteImage(id, imageId);
  }

  @GetMapping("/projects/{id}/images/{imageId}/content")
  @PreAuthorize(SecurityRoles.STAFF)
  public ResponseEntity<byte[]> image(@PathVariable UUID id, @PathVariable UUID imageId) {
    CatalogService.FileContent content = catalog.imageContent(id, imageId);
    return HttpFiles.inlineImage(content.content(), content.contentType());
  }

  @GetMapping("/projects/{projectId}/buildings")
  @PreAuthorize(SecurityRoles.STAFF)
  public List<Building> buildings(@PathVariable UUID projectId) {
    return catalog.buildings(projectId);
  }

  @PostMapping("/projects/{projectId}/buildings")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Building createBuilding(@PathVariable UUID projectId, @Valid @RequestBody BuildingBody body) {
    return catalog.createBuilding(projectId, body.name(), body.code(), body.floors());
  }

  @PutMapping("/buildings/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Building updateBuilding(@PathVariable UUID id, @Valid @RequestBody BuildingBody body) {
    return catalog.updateBuilding(id, body.name(), body.code(), body.floors());
  }

  @DeleteMapping("/buildings/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void deleteBuilding(@PathVariable UUID id) {
    catalog.deleteBuilding(id);
  }

  @GetMapping("/units")
  @PreAuthorize(SecurityRoles.STAFF)
  public PageResponse<Unit> units(
      @RequestParam(required = false) UUID projectId,
      @RequestParam(required = false) Unit.Availability availability,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return catalog.searchUnits(projectId, availability, q, page, size);
  }

  @GetMapping("/projects/{projectId}/units")
  @PreAuthorize(SecurityRoles.STAFF)
  public List<Unit> unitsOf(@PathVariable UUID projectId) {
    return catalog.unitsOf(projectId);
  }

  @PostMapping("/projects/{projectId}/units")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Unit createUnit(@PathVariable UUID projectId, @Valid @RequestBody UnitBody body) {
    return catalog.createUnit(projectId, body.toCommand());
  }

  @PutMapping("/units/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Unit updateUnit(@PathVariable UUID id, @Valid @RequestBody UnitBody body) {
    return catalog.updateUnit(id, body.toCommand());
  }

  @PatchMapping("/units/{id}/availability")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Unit availability(@PathVariable UUID id, @Valid @RequestBody AvailabilityBody body) {
    return catalog.changeAvailability(id, body.availability());
  }

  @DeleteMapping("/units/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public void deleteUnit(@PathVariable UUID id) {
    catalog.deleteUnit(id);
  }

  public record ProjectBody(
      @NotBlank @Size(max = 180) String name,
      @Size(max = 8000) String description,
      @NotNull Project.DevelopmentType developmentType,
      @NotBlank @Size(max = 2) String provinceCode,
      @NotNull UUID municipalityId,
      @Size(max = 240) String address,
      BigDecimal latitude,
      BigDecimal longitude,
      LocalDate estimatedStart,
      LocalDate estimatedDelivery,
      @NotNull Project.ProjectStatus status,
      List<@Size(max = 80) String> amenities,
      List<@Size(max = 80) String> features,
      @Size(max = 4000) String commercialInfo,
      @Size(max = 4000) String internalNotes,
      BigDecimal minPrice,
      @Size(max = 3) String currency) {

    CatalogService.SaveProjectRequest toCommand() {
      return new CatalogService.SaveProjectRequest(
          name, description, developmentType, provinceCode, municipalityId, address, latitude, longitude,
          estimatedStart, estimatedDelivery, status, amenities, features, commercialInfo, internalNotes, minPrice, currency);
    }
  }

  public record PublicationBody(@NotNull Project.PublicationStatus publication, boolean featured) {}

  public record NoticeBody(@NotBlank @Size(max = 1000) String message) {}

  public record BuildingBody(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 40) String code, Integer floors) {}

  public record UnitBody(
      UUID buildingId,
      @NotBlank @Size(max = 40) String code,
      @NotNull Unit.PropertyType propertyType,
      Integer bedrooms,
      BigDecimal bathrooms,
      BigDecimal areaM2,
      @Size(max = 40) String levelLabel,
      @NotNull BigDecimal price,
      @Size(max = 3) String currency,
      List<@Size(max = 80) String> features,
      boolean publiclyVisible) {

    CatalogService.SaveUnitRequest toCommand() {
      return new CatalogService.SaveUnitRequest(
          buildingId, code, propertyType, bedrooms, bathrooms, areaM2, levelLabel, price, currency, features, publiclyVisible);
    }
  }

  public record AvailabilityBody(@NotNull Unit.Availability availability) {}
}
