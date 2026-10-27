package rd.alzada.documents;

import rd.alzada.common.HttpFiles;
import rd.alzada.common.SecurityRoles;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

  private final DocumentService documents;

  public DocumentController(DocumentService documents) {
    this.documents = documents;
  }

  @GetMapping
  @PreAuthorize(SecurityRoles.STAFF)
  public List<DocumentView> list(@RequestParam DocumentFile.OwnerType ownerType, @RequestParam UUID ownerId) {
    return documents.list(ownerType, ownerId).stream().map(DocumentView::from).toList();
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(SecurityRoles.STAFF)
  public DocumentView store(
      @RequestParam DocumentFile.OwnerType ownerType,
      @RequestParam UUID ownerId,
      @RequestParam(defaultValue = "PRIVATE") DocumentFile.Visibility visibility,
      @RequestParam("file") MultipartFile file) {
    return DocumentView.from(documents.store(ownerType, ownerId, visibility, file));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize(SecurityRoles.STAFF)
  public void delete(@PathVariable UUID id) {
    documents.delete(id);
  }

  @GetMapping("/{id}/content")
  @PreAuthorize("hasAnyRole('COMPANY_ADMIN','SALES_ADVISOR','FINANCE','BUYER')")
  public ResponseEntity<byte[]> content(@PathVariable UUID id) {
    DocumentFile document = documents.requireReadable(id);
    return HttpFiles.download(documents.content(document), document.getContentType(), document.getFileName());
  }

  public record DocumentView(
      UUID id,
      DocumentFile.OwnerType ownerType,
      UUID ownerId,
      String fileName,
      String contentType,
      long sizeBytes,
      DocumentFile.Visibility visibility,
      Instant createdAt) {

    public static DocumentView from(DocumentFile document) {
      return new DocumentView(
          document.getId(),
          document.getOwnerType(),
          document.getOwnerId(),
          document.getFileName(),
          document.getContentType(),
          document.getSizeBytes(),
          document.getVisibility(),
          document.getCreatedAt());
    }
  }
}
