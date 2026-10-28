package rd.alzada.documents;

import rd.alzada.common.ApiException;
import rd.alzada.common.SecurityRoles;
import rd.alzada.common.SecuritySupport;
import rd.alzada.config.AppProperties;
import rd.alzada.contracts.ContractPartyRepository;
import rd.alzada.customers.Customer;
import rd.alzada.customers.CustomerRepository;
import rd.alzada.payments.PaymentRepository;
import rd.alzada.storage.FileRules;
import rd.alzada.storage.ObjectStorage;
import rd.alzada.storage.StoredFile;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  private final DocumentRepository documents;
  private final ContractPartyRepository parties;
  private final CustomerRepository customers;
  private final PaymentRepository payments;
  private final ObjectStorage storage;
  private final AppProperties properties;

  public DocumentService(
      DocumentRepository documents,
      ContractPartyRepository parties,
      CustomerRepository customers,
      PaymentRepository payments,
      ObjectStorage storage,
      AppProperties properties) {
    this.documents = documents;
    this.parties = parties;
    this.customers = customers;
    this.payments = payments;
    this.storage = storage;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public List<DocumentFile> list(DocumentFile.OwnerType ownerType, UUID ownerId) {
    assertCanRead(ownerType, ownerId);
    return documents.findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtDesc(SecuritySupport.tenantId(), ownerType, ownerId);
  }

  @Transactional
  public DocumentFile store(DocumentFile.OwnerType ownerType, UUID ownerId, DocumentFile.Visibility visibility, MultipartFile file) {
    assertCanWrite(ownerType, ownerId);
    UUID tenantId = SecuritySupport.tenantId();
    StoredFile stored = FileRules.document(FileRules.read(file, properties.uploads().documentMaxBytes()));
    String original = file.getOriginalFilename() == null ? "documento." + stored.extension() : file.getOriginalFilename();
    String safeName = original.replaceAll("[^\\p{L}\\p{N}._ -]", "").trim();
    if (safeName.isBlank()) {
      safeName = "documento." + stored.extension();
    }
    String key = "companies/" + tenantId + "/documents/" + UUID.randomUUID() + "." + stored.extension();
    storage.put(key, stored.content(), stored.contentType());
    DocumentFile document = new DocumentFile();
    document.setTenantId(tenantId);
    document.setOwnerType(ownerType);
    document.setOwnerId(ownerId);
    document.setObjectKey(key);
    document.setFileName(safeName.length() > 240 ? safeName.substring(0, 240) : safeName);
    document.setContentType(stored.contentType());
    document.setSizeBytes(stored.content().length);
    document.setVisibility(visibility == null ? DocumentFile.Visibility.PRIVATE : visibility);
    document.setUploadedBy(SecuritySupport.currentUserId());
    documents.save(document);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) {
          storage.delete(key);
        }
      }
    });
    return document;
  }

  @Transactional
  public void delete(UUID id) {
    DocumentFile document = require(id);
    if (document.getOwnerType() == DocumentFile.OwnerType.PAYMENT) {
      throw ApiException.conflict("Los comprobantes de pago no se eliminan. Rechace o reverse el pago");
    }
    assertCanWrite(document.getOwnerType(), document.getOwnerId());
    document.setDeletedAt(java.time.Instant.now());
  }

  @Transactional(readOnly = true)
  public DocumentFile requireReadable(UUID id) {
    DocumentFile document = require(id);
    assertCanRead(document.getOwnerType(), document.getOwnerId());
    if (SecuritySupport.hasRole(SecurityRoles.BUYER) && document.getVisibility() != DocumentFile.Visibility.BUYER_VISIBLE) {
      throw ApiException.forbidden("Este documento no está disponible para el comprador");
    }
    return document;
  }

  public byte[] content(DocumentFile document) {
    return storage.get(document.getObjectKey());
  }

  private DocumentFile require(UUID id) {
    return documents.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("El documento no existe"));
  }

  private void assertCanRead(DocumentFile.OwnerType ownerType, UUID ownerId) {
    UUID tenantId = SecuritySupport.tenantId();
    if (SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      return;
    }
    if (SecuritySupport.hasRole(SecurityRoles.BUYER)) {
      if (!buyerOwns(ownerType, ownerId, tenantId)) {
        throw ApiException.forbidden("No puede consultar este documento");
      }
      return;
    }
    if (SecuritySupport.hasRole(SecurityRoles.FINANCE)) {
      if (ownerType == DocumentFile.OwnerType.CONTRACT || ownerType == DocumentFile.OwnerType.PAYMENT || ownerType == DocumentFile.OwnerType.CUSTOMER || ownerType == DocumentFile.OwnerType.COMPANY) {
        return;
      }
    }
    if (SecuritySupport.hasRole(SecurityRoles.SALES_ADVISOR) && (ownerType == DocumentFile.OwnerType.PROJECT || ownerType == DocumentFile.OwnerType.CUSTOMER || ownerType == DocumentFile.OwnerType.COMPANY)) {
      return;
    }
    throw ApiException.forbidden("No puede consultar este documento");
  }

  private void assertCanWrite(DocumentFile.OwnerType ownerType, UUID ownerId) {
    if (SecuritySupport.hasRole(SecurityRoles.COMPANY_ADMIN)) {
      return;
    }
    if (SecuritySupport.hasRole(SecurityRoles.FINANCE)
        && (ownerType == DocumentFile.OwnerType.CONTRACT || ownerType == DocumentFile.OwnerType.PAYMENT || ownerType == DocumentFile.OwnerType.CUSTOMER)) {
      return;
    }
    if (SecuritySupport.hasRole(SecurityRoles.BUYER) && ownerType == DocumentFile.OwnerType.PAYMENT && buyerOwns(ownerType, ownerId, SecuritySupport.tenantId())) {
      return;
    }
    throw ApiException.forbidden("No puede adjuntar este documento");
  }

  private boolean buyerOwns(DocumentFile.OwnerType ownerType, UUID ownerId, UUID tenantId) {
    Customer customer = customers.findByTenantIdAndUserId(tenantId, SecuritySupport.currentUserId()).orElse(null);
    if (customer == null) {
      return false;
    }
    if (ownerType == DocumentFile.OwnerType.CUSTOMER) {
      return customer.getId().equals(ownerId);
    }
    if (ownerType == DocumentFile.OwnerType.CONTRACT) {
      return parties.findByContractIdAndCustomerId(ownerId, customer.getId()).isPresent();
    }
    if (ownerType == DocumentFile.OwnerType.PAYMENT) {
      return payments.findByIdAndTenantId(ownerId, tenantId)
          .map(payment -> parties.findByContractIdAndCustomerId(payment.getContractId(), customer.getId()).isPresent())
          .orElse(false);
    }
    return false;
  }
}
