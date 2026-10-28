package rd.alzada.tenants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  List<Membership> findByUserIdAndStatus(UUID userId, Membership.Status status);

  List<Membership> findByCompanyIdOrderByCreatedAtAsc(UUID companyId);

  Optional<Membership> findByIdAndCompanyId(UUID id, UUID companyId);

  boolean existsByUserIdAndCompanyIdAndRoleAndStatus(UUID userId, UUID companyId, CompanyRole role, Membership.Status status);

  long countByCompanyIdAndRoleAndStatus(UUID companyId, CompanyRole role, Membership.Status status);

  List<Membership> findByCompanyIdAndRoleAndStatus(UUID companyId, CompanyRole role, Membership.Status status);
}
