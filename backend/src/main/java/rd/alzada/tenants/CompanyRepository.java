package rd.alzada.tenants;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

  Optional<Company> findBySlug(String slug);

  boolean existsBySlug(String slug);

  long countByStatus(Company.Status status);

  @Query("""
      select company from Company company
      where (:query is null or lower(company.searchText) like :query escape '\\')
        and (:status is null or company.status = :status)
      """)
  Page<Company> search(@Param("query") String query, @Param("status") Company.Status status, Pageable pageable);
}
