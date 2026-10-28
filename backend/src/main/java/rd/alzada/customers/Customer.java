package rd.alzada.customers;

import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "customers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Customer extends TenantEntity {

  private UUID userId;

  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(length = 180)
  private String email;

  @Column(length = 30)
  private String phone;

  @Column(length = 20)
  private String nationalId;

  @Column(length = 240)
  private String address;

  private Instant deletedAt;
}
