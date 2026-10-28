package rd.alzada.projects;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_images")
@Getter
@Setter
public class ProjectImage extends TenantEntity {

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false, length = 400)
  private String objectKey;

  @Column(nullable = false, length = 80)
  private String contentType;

  @Column(nullable = false)
  private int sortOrder;

  @Column(length = 180)
  private String altText;
}
