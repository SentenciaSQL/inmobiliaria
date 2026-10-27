package rd.alzada.inventory;

import rd.alzada.common.StringListConverter;
import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "units")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Unit extends TenantEntity {

  @Column(nullable = false)
  private UUID projectId;

  private UUID buildingId;

  @Column(nullable = false, length = 40)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PropertyType propertyType;

  private Integer bedrooms;

  @Column(precision = 4, scale = 1)
  private BigDecimal bathrooms;

  @Column(precision = 10, scale = 2)
  private BigDecimal areaM2;

  @Column(length = 40)
  private String levelLabel;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Convert(converter = StringListConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private List<String> features = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Availability availability = Availability.AVAILABLE;

  @Column(nullable = false)
  private boolean publiclyVisible = true;

  @Version
  @Column(nullable = false)
  private long rowVersion;

  private Instant deletedAt;

  public enum PropertyType {
    APARTMENT,
    HOUSE,
    PENTHOUSE,
    LOT,
    COMMERCIAL,
    PARKING
  }

  public enum Availability {
    AVAILABLE,
    RESERVED,
    SOLD,
    OFF_MARKET
  }
}
