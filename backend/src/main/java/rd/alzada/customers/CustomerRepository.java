package rd.alzada.customers;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Customer> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  @Query("""
      select customer from Customer customer
      where customer.tenantId = :tenantId
        and (:query is null or lower(customer.fullName) like :query escape '\\'
            or lower(coalesce(customer.email, '')) like :query escape '\\'
            or lower(coalesce(customer.nationalId, '')) like :query escape '\\')
      """)
  Page<Customer> search(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);
}
