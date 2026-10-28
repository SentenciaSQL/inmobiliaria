package rd.alzada.contracts;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

  Optional<Contract> findByIdAndTenantId(UUID id, UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select contract from Contract contract where contract.id = :id and contract.tenantId = :tenantId")
  Optional<Contract> lock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  boolean existsByUnitIdAndStatusIn(UUID unitId, Collection<Contract.Status> statuses);

  Optional<Contract> findByUnitIdAndStatus(UUID unitId, Contract.Status status);

  @Query("""
      select contract from Contract contract
      where contract.tenantId = :tenantId
        and (:status is null or contract.status = :status)
        and (:projectId is null or contract.projectId = :projectId)
      """)
  Page<Contract> search(
      @Param("tenantId") UUID tenantId,
      @Param("status") Contract.Status status,
      @Param("projectId") UUID projectId,
      Pageable pageable);

  @Query("""
      select contract from Contract contract, ContractParty party
      where party.contractId = contract.id
        and party.customerId = :customerId
        and contract.tenantId = :tenantId
        and contract.status in (rd.alzada.contracts.Contract.Status.ACTIVE, rd.alzada.contracts.Contract.Status.COMPLETED, rd.alzada.contracts.Contract.Status.DRAFT)
      order by contract.agreementDate desc
      """)
  List<Contract> findForCustomer(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

  @Query("""
      select distinct contract from Contract contract, ContractParty party, Customer customer
      where party.contractId = contract.id
        and party.customerId = customer.id
        and customer.userId = :userId
        and contract.tenantId = :tenantId
        and contract.status <> rd.alzada.contracts.Contract.Status.DRAFT
      order by contract.agreementDate desc
      """)
  List<Contract> findForBuyer(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  List<Contract> findByProjectIdAndTenantIdAndStatus(UUID projectId, UUID tenantId, Contract.Status status);
}
