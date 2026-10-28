package rd.alzada.marketplace;

import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.RateLimiter;
import rd.alzada.common.TenantContext;
import rd.alzada.common.Texts;
import rd.alzada.config.AppProperties;
import rd.alzada.inventory.Building;
import rd.alzada.inventory.BuildingRepository;
import rd.alzada.inventory.Unit;
import rd.alzada.inventory.UnitRepository;
import rd.alzada.leads.CrmService;
import rd.alzada.projects.Project;
import rd.alzada.projects.ProjectImage;
import rd.alzada.projects.ProjectImageRepository;
import rd.alzada.projects.ProjectRepository;
import rd.alzada.storage.ObjectStorage;
import rd.alzada.tenants.Company;
import rd.alzada.tenants.CompanyRepository;
import rd.alzada.tenants.CompanyService;
import rd.alzada.tenants.LocationCatalog;
import rd.alzada.tenants.Municipality;
import rd.alzada.tenants.Province;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceService {

  private final ProjectRepository projects;
  private final ProjectImageRepository images;
  private final UnitRepository units;
  private final BuildingRepository buildings;
  private final CompanyRepository companies;
  private final LocationCatalog locations;
  private final CrmService crm;
  private final ObjectStorage storage;
  private final AppProperties properties;
  private final RateLimiter rateLimiter = new RateLimiter();

  public MarketplaceService(
      ProjectRepository projects,
      ProjectImageRepository images,
      UnitRepository units,
      BuildingRepository buildings,
      CompanyRepository companies,
      LocationCatalog locations,
      CrmService crm,
      ObjectStorage storage,
      AppProperties properties) {
    this.projects = projects;
    this.images = images;
    this.units = units;
    this.buildings = buildings;
    this.companies = companies;
    this.locations = locations;
    this.crm = crm;
    this.storage = storage;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public List<ProvinceNode> locations() {
    return locations.provinces().stream().map(province -> new ProvinceNode(
        province.getCode(),
        province.getName(),
        locations.municipalities().stream().filter(item -> item.getProvinceCode().equals(province.getCode())).map(this::municipality).toList()
    )).toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<ProjectCard> search(
      String query,
      String province,
      UUID municipalityId,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      Project.DevelopmentType type,
      Project.ProjectStatus status,
      boolean availableOnly,
      int page,
      int size) {
    return PageResponse.of(projects.marketplace(
        Texts.like(query),
        Texts.blankToNull(province),
        municipalityId,
        minPrice,
        maxPrice,
        type,
        status,
        availableOnly,
        Pages.unsorted(page, size == 0 ? 12 : size)).map(this::card));
  }

  @Transactional(readOnly = true)
  public List<ProjectCard> featured() {
    return projects.featured(PageRequest.of(0, 6)).stream().map(this::card).toList();
  }

  @Transactional(readOnly = true)
  public ProjectPage project(String slug) {
    Project project = projects.findPublishedBySlug(slug).orElseThrow(() -> ApiException.notFound("El proyecto no está publicado"));
    Company company = companies.findById(project.getTenantId()).orElseThrow(() -> ApiException.notFound("El proyecto no está publicado"));
    List<ProjectImage> gallery = images.findByProjectIdOrderBySortOrderAsc(project.getId());
    List<Building> blocks = buildings.findByProjectIdOrderByNameAsc(project.getId());
    Map<UUID, String> buildingNames = blocks.stream().collect(Collectors.toMap(Building::getId, Building::getName));
    List<UnitCard> visible = units.findVisible(project.getId()).stream().map(unit -> unitCard(unit, buildingNames.get(unit.getBuildingId()))).toList();
    return new ProjectPage(card(project), project.getDescription(), project.getAddress(), project.getLatitude(), project.getLongitude(),
        project.getEstimatedStart(), project.getEstimatedDelivery(), project.getAmenities(), project.getFeatures(), project.getCommercialInfo(),
        gallery.stream().map(image -> new ImageCard(image.getId(), image.getAltText(), "/api/v1/public/media/projects/" + project.getId() + "/images/" + image.getId())).toList(),
        blocks.stream().map(building -> new BuildingCard(building.getId(), building.getName(), building.getCode(), building.getFloors())).toList(),
        visible,
        new CompanyCard(company.getId(), company.getSlug(), company.getCommercialName(), company.getDescription(), CompanyService.logoUrl(company), company.getPublicPhone(), company.getPublicEmail(), company.getWebsite(), company.getPublicContact()));
  }

  @Transactional(readOnly = true)
  public CompanyPage company(String slug) {
    Company company = companies.findBySlug(slug).orElseThrow(() -> ApiException.notFound("La constructora no está disponible"));
    List<ProjectCard> published = projects.publishedByCompanySlug(slug).stream().map(this::card).toList();
    return new CompanyPage(company.getId(), company.getSlug(), company.getCommercialName(), company.getDescription(),
        CompanyService.logoUrl(company), company.getPublicPhone(), company.getPublicEmail(), company.getWebsite(), company.getPublicContact(),
        locations.provinceName(company.getProvinceCode()), locations.municipalityName(company.getMunicipalityId()), company.getAddress(), published);
  }

  public void inquire(String slug, String name, String email, String phone, UUID unitId, String message, String ip) {
    if (!rateLimiter.tryAcquire("inquiry:" + ip, 8, Duration.ofHours(1))) {
      throw ApiException.badRequest("Ya recibimos varias solicitudes desde esta conexión. Intente más tarde");
    }
    Project project = projects.findPublishedBySlug(slug).orElseThrow(() -> ApiException.notFound("El proyecto no está publicado"));
    if (unitId != null) {
      Unit unit = units.findById(unitId).filter(item -> item.getProjectId().equals(project.getId())).orElseThrow(() -> ApiException.badRequest("La unidad no está publicada"));
      if (!unit.isPubliclyVisible() || unit.getAvailability() == Unit.Availability.OFF_MARKET) {
        throw ApiException.badRequest("La unidad no está publicada");
      }
    }
    TenantContext.asTenant(null, project.getTenantId());
    try {
      crm.createMarketplaceLead(project.getTenantId(), project.getId(), unitId, name, email, phone, message);
    } finally {
      TenantContext.asMarketplace(null);
    }
  }

  @Transactional(readOnly = true)
  public StoredMedia logo(UUID companyId) {
    Company company = companies.findById(companyId).orElseThrow(() -> ApiException.notFound("El archivo no está disponible"));
    if (company.getLogoObjectKey() == null) {
      throw ApiException.notFound("La constructora no tiene logo");
    }
    return new StoredMedia(storage.get(company.getLogoObjectKey()), company.getLogoContentType() == null ? "image/png" : company.getLogoContentType());
  }

  @Transactional(readOnly = true)
  public StoredMedia projectImage(UUID projectId, UUID imageId) {
    projects.findById(projectId).filter(project -> project.getPublicationStatus() == Project.PublicationStatus.PUBLISHED)
        .orElseThrow(() -> ApiException.notFound("La fotografía no está disponible"));
    ProjectImage image = images.findById(imageId).filter(item -> item.getProjectId().equals(projectId))
        .orElseThrow(() -> ApiException.notFound("La fotografía no está disponible"));
    return new StoredMedia(storage.get(image.getObjectKey()), image.getContentType());
  }

  @Transactional(readOnly = true)
  public String sitemap() {
    String base = properties.publicUrl().replaceAll("/$", "");
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    xml.append("<url><loc>").append(base).append("/</loc></url>\n");
    projects.marketplace(null, null, null, null, null, null, null, false, PageRequest.of(0, 100)).forEach(project ->
        xml.append("<url><loc>").append(base).append("/proyectos/").append(project.getSlug()).append("</loc></url>\n"));
    xml.append("</urlset>");
    return xml.toString();
  }

  private ProjectCard card(Project project) {
    Company company = companies.findById(project.getTenantId()).orElse(null);
    ProjectImage cover = images.findByProjectIdOrderBySortOrderAsc(project.getId()).stream().findFirst().orElse(null);
    return new ProjectCard(
        project.getId(),
        project.getSlug(),
        project.getName(),
        project.getDevelopmentType(),
        project.getStatus(),
        locations.provinceName(project.getProvinceCode()),
        locations.municipalityName(project.getMunicipalityId()),
        project.getMinPrice(),
        project.getCurrency(),
        project.getEstimatedDelivery(),
        project.isFeatured(),
        company == null ? null : company.getCommercialName(),
        company == null ? null : company.getSlug(),
        company == null ? null : CompanyService.logoUrl(company),
        cover == null ? null : "/api/v1/public/media/projects/" + project.getId() + "/images/" + cover.getId());
  }

  private UnitCard unitCard(Unit unit, String buildingName) {
    return new UnitCard(unit.getId(), unit.getCode(), unit.getPropertyType(), unit.getBedrooms(), unit.getBathrooms(), unit.getAreaM2(), unit.getLevelLabel(), unit.getPrice(), unit.getCurrency(), unit.getAvailability(), unit.getFeatures(), buildingName);
  }

  private MunicipalityNode municipality(Municipality municipality) {
    return new MunicipalityNode(municipality.getId(), municipality.getName());
  }

  public record ProvinceNode(String code, String name, List<MunicipalityNode> municipalities) {}
  public record MunicipalityNode(UUID id, String name) {}
  public record ProjectCard(
      UUID id,
      String slug,
      String name,
      Project.DevelopmentType developmentType,
      Project.ProjectStatus status,
      String provinceName,
      String municipalityName,
      BigDecimal minPrice,
      String currency,
      java.time.LocalDate estimatedDelivery,
      boolean featured,
      String companyName,
      String companySlug,
      String companyLogoUrl,
      String coverUrl) {}
  public record ImageCard(UUID id, String altText, String url) {}
  public record BuildingCard(UUID id, String name, String code, Integer floors) {}
  public record UnitCard(
      UUID id,
      String code,
      Unit.PropertyType propertyType,
      Integer bedrooms,
      BigDecimal bathrooms,
      BigDecimal areaM2,
      String levelLabel,
      BigDecimal price,
      String currency,
      Unit.Availability availability,
      List<String> features,
      String buildingName) {}
  public record CompanyCard(UUID id, String slug, String commercialName, String description, String logoUrl, String publicPhone, String publicEmail, String website, String publicContact) {}
  public record ProjectPage(
      ProjectCard summary,
      String description,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      java.time.LocalDate estimatedStart,
      java.time.LocalDate estimatedDelivery,
      List<String> amenities,
      List<String> features,
      String commercialInfo,
      List<ImageCard> images,
      List<BuildingCard> buildings,
      List<UnitCard> units,
      CompanyCard company) {}
  public record CompanyPage(
      UUID id,
      String slug,
      String commercialName,
      String description,
      String logoUrl,
      String publicPhone,
      String publicEmail,
      String website,
      String publicContact,
      String provinceName,
      String municipalityName,
      String address,
      List<ProjectCard> projects) {}
  public record StoredMedia(byte[] content, String contentType) {}
}
