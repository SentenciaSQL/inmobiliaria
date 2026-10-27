package rd.alzada.marketplace;

import rd.alzada.common.HttpFiles;
import rd.alzada.common.PageResponse;
import rd.alzada.inventory.Unit;
import rd.alzada.projects.Project;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

  private final MarketplaceService marketplace;

  public PublicController(MarketplaceService marketplace) {
    this.marketplace = marketplace;
  }

  @GetMapping("/api/v1/public/locations")
  public List<MarketplaceService.ProvinceNode> locations() {
    return marketplace.locations();
  }

  @GetMapping("/api/v1/public/projects")
  public PageResponse<MarketplaceService.ProjectCard> projects(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String province,
      @RequestParam(required = false) UUID municipalityId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Project.DevelopmentType type,
      @RequestParam(required = false) Project.ProjectStatus status,
      @RequestParam(required = false) Unit.Availability availability,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    boolean availableOnly = availability == Unit.Availability.AVAILABLE;
    return marketplace.search(q, province, municipalityId, minPrice, maxPrice, type, status, availableOnly, page, size);
  }

  @GetMapping("/api/v1/public/projects/featured")
  public List<MarketplaceService.ProjectCard> featured() {
    return marketplace.featured();
  }

  @GetMapping("/api/v1/public/projects/{slug}")
  public MarketplaceService.ProjectPage project(@PathVariable String slug) {
    return marketplace.project(slug);
  }

  @PostMapping("/api/v1/public/projects/{slug}/inquiries")
  public void inquire(@PathVariable String slug, @Valid @RequestBody InquiryRequest request, HttpServletRequest http) {
    marketplace.inquire(slug, request.name(), request.email(), request.phone(), request.unitId(), request.message(), http.getRemoteAddr());
  }

  @GetMapping("/api/v1/public/companies/{slug}")
  public MarketplaceService.CompanyPage company(@PathVariable String slug) {
    return marketplace.company(slug);
  }

  @GetMapping("/api/v1/public/media/companies/{id}/logo")
  public ResponseEntity<byte[]> logo(@PathVariable UUID id) {
    MarketplaceService.StoredMedia media = marketplace.logo(id);
    return HttpFiles.inlineImage(media.content(), media.contentType());
  }

  @GetMapping("/api/v1/public/media/projects/{projectId}/images/{imageId}")
  public ResponseEntity<byte[]> image(@PathVariable UUID projectId, @PathVariable UUID imageId) {
    MarketplaceService.StoredMedia media = marketplace.projectImage(projectId, imageId);
    return HttpFiles.inlineImage(media.content(), media.contentType());
  }

  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String sitemap() {
    return marketplace.sitemap();
  }

  public record InquiryRequest(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Email @Size(max = 180) String email,
      @Size(max = 30) String phone,
      UUID unitId,
      @Size(max = 2000) String message) {}
}
