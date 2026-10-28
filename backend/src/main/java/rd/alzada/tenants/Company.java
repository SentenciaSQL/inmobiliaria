package rd.alzada.tenants;

import rd.alzada.common.BaseEntity;
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
@Table(name = "companies")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class Company extends BaseEntity {

  @Column(nullable = false, length = 160)
  private String slug;

  @Column(nullable = false, length = 160)
  private String commercialName;

  @Column(nullable = false, length = 200)
  private String legalName;

  @Column(length = 20)
  private String rnc;

  @Column(length = 30)
  private String phone;

  @Column(length = 180)
  private String email;

  @Column(length = 240)
  private String address;

  @Column(length = 2)
  private String provinceCode;

  private UUID municipalityId;

  @Column(length = 200)
  private String website;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 400)
  private String logoObjectKey;

  @Column(length = 80)
  private String logoContentType;

  @Column(length = 30)
  private String publicPhone;

  @Column(length = 180)
  private String publicEmail;

  @Column(columnDefinition = "text")
  private String publicContact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  @Column(length = 400)
  private String searchText;

  private Instant deletedAt;

  public enum Status {
    ACTIVE,
    SUSPENDED
  }
}
