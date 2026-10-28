package rd.alzada.projects;

import rd.alzada.common.StringListConverter;
import rd.alzada.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "projects")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Project extends TenantEntity {

  @Column(nullable = false, length = 180)
  private String name;

  @Column(nullable = false, length = 180)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private DevelopmentType developmentType;

  @Column(length = 2)
  private String provinceCode;

  private UUID municipalityId;

  @Column(length = 240)
  private String address;

  @Column(precision = 9, scale = 6)
  private BigDecimal latitude;

  @Column(precision = 9, scale = 6)
  private BigDecimal longitude;

  private LocalDate estimatedStart;

  private LocalDate estimatedDelivery;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ProjectStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

  @Column(nullable = false)
  private boolean featured;

  @Convert(converter = StringListConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private List<String> amenities = new ArrayList<>();

  @Convert(converter = StringListConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private List<String> features = new ArrayList<>();

  @Column(columnDefinition = "text")
  private String commercialInfo;

  @Column(columnDefinition = "text")
  private String internalNotes;

  @Column(precision = 14, scale = 2)
  private BigDecimal minPrice;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  private Instant publishedAt;

  @Column(length = 500)
  private String searchText;

  private Instant deletedAt;

  public enum DevelopmentType {
    APARTMENT_BUILDING,
    HOUSE_COMMUNITY,
    MIXED,
    LOTS,
    COMMERCIAL
  }

  public enum ProjectStatus {
    PLANNING,
    IN_CONSTRUCTION,
    READY,
    DELIVERED,
    PAUSED
  }

  public enum PublicationStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN
  }
}
