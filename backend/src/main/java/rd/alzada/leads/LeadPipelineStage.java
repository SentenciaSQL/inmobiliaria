package rd.alzada.leads;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lead_pipeline_stages")
@Getter
@Setter
public class LeadPipelineStage extends TenantEntity {

  @Column(nullable = false, length = 40)
  private String code;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean closed;

  @Column(nullable = false)
  private boolean systemDefault;
}
