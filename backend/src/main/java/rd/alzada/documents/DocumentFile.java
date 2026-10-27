package rd.alzada.documents;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "documents")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class DocumentFile extends TenantEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OwnerType ownerType;

  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false, length = 400)
  private String objectKey;

  @Column(nullable = false, length = 240)
  private String fileName;

  @Column(nullable = false, length = 120)
  private String contentType;

  @Column(nullable = false)
  private long sizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Visibility visibility = Visibility.PRIVATE;

  private UUID uploadedBy;

  private Instant deletedAt;

  public enum OwnerType {
    COMPANY,
    PROJECT,
    CONTRACT,
    CUSTOMER,
    PAYMENT
  }

  public enum Visibility {
    PRIVATE,
    BUYER_VISIBLE
  }
}
