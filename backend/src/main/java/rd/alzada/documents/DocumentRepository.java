package rd.alzada.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentFile, UUID> {

  Optional<DocumentFile> findByIdAndTenantId(UUID id, UUID tenantId);

  List<DocumentFile> findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtDesc(
      UUID tenantId, DocumentFile.OwnerType ownerType, UUID ownerId);
}
