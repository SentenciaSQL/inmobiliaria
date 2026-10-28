package rd.alzada.contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractPartyRepository extends JpaRepository<ContractParty, UUID> {

  List<ContractParty> findByContractIdAndTenantId(UUID contractId, UUID tenantId);

  Optional<ContractParty> findByContractIdAndCustomerId(UUID contractId, UUID customerId);

  boolean existsByContractIdAndCustomerId(UUID contractId, UUID customerId);

  long countByContractId(UUID contractId);
}
