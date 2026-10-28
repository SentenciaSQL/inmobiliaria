package rd.alzada.projects;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);

  boolean existsBySlug(String slug);

  long countByTenantId(UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select project from Project project where project.id = :id and project.tenantId = :tenantId")
  Optional<Project> lock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query("""
      select project from Project project
      where project.tenantId = :tenantId
        and (:query is null or lower(project.searchText) like :query escape '\\')
        and (:status is null or project.status = :status)
        and (:publication is null or project.publicationStatus = :publication)
      """)
  Page<Project> search(
      @Param("tenantId") UUID tenantId,
      @Param("query") String query,
      @Param("status") Project.ProjectStatus status,
      @Param("publication") Project.PublicationStatus publication,
      Pageable pageable);

  @Query(value = """
      select project from Project project, Company company
      where company.id = project.tenantId
        and company.status = rd.alzada.tenants.Company.Status.ACTIVE
        and project.publicationStatus = rd.alzada.projects.Project.PublicationStatus.PUBLISHED
        and (:query is null or lower(project.searchText) like :query escape '\\' or lower(company.searchText) like :query escape '\\')
        and (:province is null or project.provinceCode = :province)
        and (:municipalityId is null or project.municipalityId = :municipalityId)
        and (:minPrice is null or project.minPrice >= :minPrice)
        and (:maxPrice is null or project.minPrice <= :maxPrice)
        and (:type is null or project.developmentType = :type)
        and (:projectStatus is null or project.status = :projectStatus)
        and (:availableOnly = false or exists (
            select unit.id from Unit unit
            where unit.projectId = project.id
              and unit.availability = rd.alzada.inventory.Unit.Availability.AVAILABLE
              and unit.publiclyVisible = true))
      order by project.featured desc, project.publishedAt desc
      """,
      countQuery = """
      select count(project) from Project project, Company company
      where company.id = project.tenantId
        and company.status = rd.alzada.tenants.Company.Status.ACTIVE
        and project.publicationStatus = rd.alzada.projects.Project.PublicationStatus.PUBLISHED
        and (:query is null or lower(project.searchText) like :query escape '\\' or lower(company.searchText) like :query escape '\\')
        and (:province is null or project.provinceCode = :province)
        and (:municipalityId is null or project.municipalityId = :municipalityId)
        and (:minPrice is null or project.minPrice >= :minPrice)
        and (:maxPrice is null or project.minPrice <= :maxPrice)
        and (:type is null or project.developmentType = :type)
        and (:projectStatus is null or project.status = :projectStatus)
        and (:availableOnly = false or exists (
            select unit.id from Unit unit
            where unit.projectId = project.id
              and unit.availability = rd.alzada.inventory.Unit.Availability.AVAILABLE
              and unit.publiclyVisible = true))
      """)
  Page<Project> marketplace(
      @Param("query") String query,
      @Param("province") String province,
      @Param("municipalityId") UUID municipalityId,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("type") Project.DevelopmentType type,
      @Param("projectStatus") Project.ProjectStatus projectStatus,
      @Param("availableOnly") boolean availableOnly,
      Pageable pageable);

  @Query("""
      select project from Project project, Company company
      where company.id = project.tenantId
        and company.status = rd.alzada.tenants.Company.Status.ACTIVE
        and project.publicationStatus = rd.alzada.projects.Project.PublicationStatus.PUBLISHED
        and project.featured = true
      order by project.publishedAt desc
      """)
  List<Project> featured(Pageable pageable);

  @Query("""
      select project from Project project, Company company
      where project.slug = :slug
        and company.id = project.tenantId
        and company.status = rd.alzada.tenants.Company.Status.ACTIVE
        and project.publicationStatus = rd.alzada.projects.Project.PublicationStatus.PUBLISHED
      """)
  Optional<Project> findPublishedBySlug(@Param("slug") String slug);

  @Query("""
      select project from Project project, Company company
      where company.id = project.tenantId
        and company.slug = :slug
        and company.status = rd.alzada.tenants.Company.Status.ACTIVE
        and project.publicationStatus = rd.alzada.projects.Project.PublicationStatus.PUBLISHED
      order by project.featured desc, project.publishedAt desc
      """)
  List<Project> publishedByCompanySlug(@Param("slug") String slug);

  long countByPublicationStatus(Project.PublicationStatus publicationStatus);
}
