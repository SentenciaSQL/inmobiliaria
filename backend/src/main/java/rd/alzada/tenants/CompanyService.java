package rd.alzada.tenants;

import rd.alzada.audit.AuditService;
import rd.alzada.common.ApiException;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.TenantContext;
import rd.alzada.common.Texts;
import rd.alzada.config.AppProperties;
import rd.alzada.config.RlsSession;
import rd.alzada.identity.PasswordPolicyValidator;
import rd.alzada.identity.User;
import rd.alzada.identity.UserRepository;
import rd.alzada.leads.LeadPipelineStage;
import rd.alzada.leads.LeadPipelineStageRepository;
import rd.alzada.storage.FileRules;
import rd.alzada.storage.ObjectStorage;
import rd.alzada.storage.StoredFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyService {

  private static final List<StageSeed> STAGES = List.of(
      new StageSeed("NEW", "Nuevo", 1, false),
      new StageSeed("CONTACTED", "Contactado", 2, false),
      new StageSeed("VISIT_SCHEDULED", "Visita coordinada", 3, false),
      new StageSeed("NEGOTIATION", "Negociación", 4, false),
      new StageSeed("RESERVED", "Reservado", 5, false),
      new StageSeed("WON", "Ganado", 6, true),
      new StageSeed("LOST", "Perdido", 7, true));

  private final CompanyRepository companies;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final LeadPipelineStageRepository stages;
  private final LocationCatalog locations;
  private final ObjectStorage storage;
  private final AppProperties properties;
  private final PasswordEncoder passwordEncoder;
  private final AuditService audit;
  private final RlsSession rlsSession;

  public CompanyService(
      CompanyRepository companies,
      MembershipRepository memberships,
      UserRepository users,
      LeadPipelineStageRepository stages,
      LocationCatalog locations,
      ObjectStorage storage,
      AppProperties properties,
      PasswordEncoder passwordEncoder,
      AuditService audit,
      RlsSession rlsSession) {
    this.companies = companies;
    this.memberships = memberships;
    this.users = users;
    this.stages = stages;
    this.locations = locations;
    this.storage = storage;
    this.properties = properties;
    this.passwordEncoder = passwordEncoder;
    this.audit = audit;
    this.rlsSession = rlsSession;
  }

  @Transactional(readOnly = true)
  public CompanyResponse current() {
    return toResponse(requireCompany(SecuritySupport.tenantId()));
  }

  @Transactional(readOnly = true)
  public BrandResponse brand() {
    Company company = requireCompany(SecuritySupport.tenantId());
    return new BrandResponse(
        company.getId(),
        company.getCommercialName(),
        company.getSlug(),
        logoUrl(company),
        company.getPublicPhone(),
        company.getPublicEmail(),
        company.getPublicContact());
  }

  @Transactional
  public CompanyResponse update(UpdateCompanyRequest request) {
    Company company = requireCompany(SecuritySupport.tenantId());
    apply(company, request.commercialName(), request.legalName(), request.rnc(), request.phone(), request.email(),
        request.address(), request.provinceCode(), request.municipalityId(), request.website(), request.description(),
        request.publicPhone(), request.publicEmail(), request.publicContact());
    audit.record("COMPANY_UPDATED", "COMPANY", company.getId(), "Actualizó los datos de la constructora", Map.of("name", company.getCommercialName()));
    return toResponse(company);
  }

  @Transactional
  public CompanyResponse uploadLogo(MultipartFile file) {
    Company company = requireCompany(SecuritySupport.tenantId());
    byte[] content = FileRules.read(file, properties.uploads().logoMaxBytes());
    StoredFile image = FileRules.image(content);
    String key = "companies/" + company.getId() + "/brand/logo-" + UUID.randomUUID() + "." + image.extension();
    String previous = company.getLogoObjectKey();
    storage.put(key, image.content(), image.contentType());
    company.setLogoObjectKey(key);
    company.setLogoContentType(image.contentType());
    afterStored(key, previous);
    audit.record("LOGO_UPDATED", "COMPANY", company.getId(), "Actualizó el logo de la constructora", Map.of());
    return toResponse(company);
  }

  @Transactional
  public CompanyResponse deleteLogo() {
    Company company = requireCompany(SecuritySupport.tenantId());
    String previous = company.getLogoObjectKey();
    company.setLogoObjectKey(null);
    company.setLogoContentType(null);
    if (previous != null) {
      afterStored(null, previous);
    }
    audit.record("LOGO_REMOVED", "COMPANY", company.getId(), "Eliminó el logo de la constructora", Map.of());
    return toResponse(company);
  }

  @Transactional(readOnly = true)
  public List<MemberResponse> members() {
    UUID tenantId = SecuritySupport.tenantId();
    return memberships.findByCompanyIdOrderByCreatedAtAsc(tenantId).stream().map(membership -> {
      User user = users.findById(membership.getUserId()).orElse(null);
      return new MemberResponse(
          membership.getId(),
          membership.getUserId(),
          user == null ? "" : user.getFullName(),
          user == null ? "" : user.getEmail(),
          user == null ? null : user.getPhone(),
          membership.getRole(),
          membership.getStatus());
    }).toList();
  }

  @Transactional
  public MemberResponse addMember(CreateMemberRequest request) {
    UUID tenantId = SecuritySupport.tenantId();
    if (request.role() == null) {
      throw ApiException.badRequest("Seleccione un rol");
    }
    String email = Texts.lowerEmail(request.email());
    User user = users.findByEmailIgnoreCase(email).orElse(null);
    boolean created = false;
    if (user == null) {
      if (request.password() == null || !new PasswordPolicyValidator().isValid(request.password(), null)) {
        throw ApiException.badRequest("Indique una contraseña inicial de al menos 10 caracteres, con una letra y un número");
      }
      user = new User();
      user.setEmail(email);
      user.setFullName(request.fullName().trim());
      user.setPhone(Texts.blankToNull(request.phone()));
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      user.setStatus(User.Status.ACTIVE);
      users.save(user);
      created = true;
    }
    if (memberships.existsByUserIdAndCompanyIdAndRoleAndStatus(user.getId(), tenantId, request.role(), Membership.Status.ACTIVE)) {
      throw ApiException.conflict("Esa persona ya tiene este rol en la constructora");
    }
    Membership membership = new Membership();
    membership.setUserId(user.getId());
    membership.setCompanyId(tenantId);
    membership.setRole(request.role());
    membership.setStatus(Membership.Status.ACTIVE);
    memberships.save(membership);
    audit.record("MEMBER_ADDED", "MEMBERSHIP", membership.getId(), "Agregó un usuario a la constructora", Map.of("role", request.role().name(), "created", created));
    return new MemberResponse(membership.getId(), user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), membership.getRole(), membership.getStatus());
  }

  @Transactional
  public MemberResponse updateMember(UUID membershipId, UpdateMemberRequest request) {
    UUID tenantId = SecuritySupport.tenantId();
    Membership membership = memberships.findByIdAndCompanyId(membershipId, tenantId)
        .orElseThrow(() -> ApiException.notFound("El usuario no pertenece a esta constructora"));
    if (membership.getRole() == CompanyRole.COMPANY_ADMIN
        && membership.getStatus() == Membership.Status.ACTIVE
        && (request.role() != CompanyRole.COMPANY_ADMIN || request.status() != Membership.Status.ACTIVE)
        && memberships.countByCompanyIdAndRoleAndStatus(tenantId, CompanyRole.COMPANY_ADMIN, Membership.Status.ACTIVE) <= 1) {
      throw ApiException.conflict("La constructora debe conservar al menos un administrador activo");
    }
    membership.setRole(request.role());
    membership.setStatus(request.status());
    audit.record("MEMBER_UPDATED", "MEMBERSHIP", membership.getId(), "Actualizó un usuario de la constructora", Map.of("role", request.role().name(), "status", request.status().name()));
    User user = users.findById(membership.getUserId()).orElseThrow();
    return new MemberResponse(membership.getId(), user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), membership.getRole(), membership.getStatus());
  }

  @Transactional
  public Company provision(CreateCompanyCommand command) {
    locations.require(command.provinceCode(), command.municipalityId());
    Company company = new Company();
    company.setStatus(Company.Status.ACTIVE);
    apply(company, command.commercialName(), command.legalName(), command.rnc(), command.phone(), command.email(),
        command.address(), command.provinceCode(), command.municipalityId(), command.website(), command.description(),
        command.publicPhone(), command.publicEmail(), command.publicContact());
    company.setSlug(uniqueSlug(Texts.slug(command.commercialName())));
    companies.save(company);
    audit.record("COMPANY_CREATED", "COMPANY", company.getId(), "Registró la constructora " + company.getCommercialName(), Map.of());
    TenantContext.asTenant(TenantContext.userId(), company.getId());
    rlsSession.apply();
    seedStages(company.getId());
    User admin = users.findByEmailIgnoreCase(Texts.lowerEmail(command.adminEmail())).orElse(null);
    if (admin == null) {
      if (!new PasswordPolicyValidator().isValid(command.adminPassword(), null)) {
        throw ApiException.badRequest("La contraseña del administrador debe tener al menos 10 caracteres, una letra y un número");
      }
      admin = new User();
      admin.setEmail(Texts.lowerEmail(command.adminEmail()));
      admin.setFullName(command.adminFullName().trim());
      admin.setPhone(Texts.blankToNull(command.adminPhone()));
      admin.setPasswordHash(passwordEncoder.encode(command.adminPassword()));
      admin.setStatus(User.Status.ACTIVE);
      users.save(admin);
    }
    Membership membership = new Membership();
    membership.setCompanyId(company.getId());
    membership.setUserId(admin.getId());
    membership.setRole(CompanyRole.COMPANY_ADMIN);
    membership.setStatus(Membership.Status.ACTIVE);
    memberships.save(membership);
    return company;
  }

  public void seedStages(UUID tenantId) {
    for (StageSeed seed : STAGES) {
      if (stages.existsByTenantIdAndCode(tenantId, seed.code())) {
        continue;
      }
      LeadPipelineStage stage = new LeadPipelineStage();
      stage.setTenantId(tenantId);
      stage.setCode(seed.code());
      stage.setName(seed.name());
      stage.setSortOrder(seed.order());
      stage.setClosed(seed.closed());
      stage.setSystemDefault(true);
      stages.save(stage);
    }
  }

  public CompanyResponse toResponse(Company company) {
    return new CompanyResponse(
        company.getId(),
        company.getSlug(),
        company.getCommercialName(),
        company.getLegalName(),
        company.getRnc(),
        company.getPhone(),
        company.getEmail(),
        company.getAddress(),
        company.getProvinceCode(),
        locations.provinceName(company.getProvinceCode()),
        company.getMunicipalityId(),
        locations.municipalityName(company.getMunicipalityId()),
        company.getWebsite(),
        company.getDescription(),
        logoUrl(company),
        company.getPublicPhone(),
        company.getPublicEmail(),
        company.getPublicContact(),
        company.getStatus());
  }

  public static String logoUrl(Company company) {
    if (company.getLogoObjectKey() == null) {
      return null;
    }
    return "/api/v1/public/media/companies/" + company.getId() + "/logo";
  }

  private Company requireCompany(UUID id) {
    return companies.findById(id).orElseThrow(() -> ApiException.notFound("La constructora no existe"));
  }

  private void apply(
      Company company,
      String commercialName,
      String legalName,
      String rnc,
      String phone,
      String email,
      String address,
      String provinceCode,
      UUID municipalityId,
      String website,
      String description,
      String publicPhone,
      String publicEmail,
      String publicContact) {
    locations.require(provinceCode, municipalityId);
    String normalizedRnc = Texts.blankToNull(rnc);
    if (normalizedRnc != null && !normalizedRnc.matches("^[0-9]{9}$|^[0-9]{11}$")) {
      throw ApiException.badRequest("El RNC o la cédula deben tener 9 u 11 dígitos");
    }
    company.setCommercialName(commercialName.trim());
    company.setLegalName(legalName.trim());
    company.setRnc(normalizedRnc);
    company.setPhone(Texts.blankToNull(phone));
    company.setEmail(Texts.lowerEmail(email));
    company.setAddress(Texts.blankToNull(address));
    company.setProvinceCode(provinceCode);
    company.setMunicipalityId(municipalityId);
    company.setWebsite(Texts.blankToNull(website));
    company.setDescription(Texts.blankToNull(description));
    company.setPublicPhone(Texts.blankToNull(publicPhone));
    company.setPublicEmail(Texts.lowerEmail(publicEmail));
    company.setPublicContact(Texts.blankToNull(publicContact));
    company.setSearchText(Texts.truncate(Texts.fold(company.getCommercialName() + " " + company.getLegalName()), 400));
  }

  private String uniqueSlug(String base) {
    String candidate = base;
    int suffix = 2;
    while (companies.existsBySlug(candidate)) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private void afterStored(String newKey, String previous) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      if (previous != null && !previous.equals(newKey)) {
        storage.delete(previous);
      }
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_COMMITTED) {
          if (previous != null && !previous.equals(newKey)) {
            storage.delete(previous);
          }
        } else if (newKey != null) {
          storage.delete(newKey);
        }
      }
    });
  }

  private record StageSeed(String code, String name, int order, boolean closed) {}

  public record UpdateCompanyRequest(
      String commercialName,
      String legalName,
      String rnc,
      String phone,
      String email,
      String address,
      String provinceCode,
      UUID municipalityId,
      String website,
      String description,
      String publicPhone,
      String publicEmail,
      String publicContact) {}

  public record CreateCompanyCommand(
      String commercialName,
      String legalName,
      String rnc,
      String phone,
      String email,
      String address,
      String provinceCode,
      UUID municipalityId,
      String website,
      String description,
      String publicPhone,
      String publicEmail,
      String publicContact,
      String adminFullName,
      String adminEmail,
      String adminPhone,
      String adminPassword) {}

  public record CompanyResponse(
      UUID id,
      String slug,
      String commercialName,
      String legalName,
      String rnc,
      String phone,
      String email,
      String address,
      String provinceCode,
      String provinceName,
      UUID municipalityId,
      String municipalityName,
      String website,
      String description,
      String logoUrl,
      String publicPhone,
      String publicEmail,
      String publicContact,
      Company.Status status) {}

  public record BrandResponse(
      UUID id,
      String commercialName,
      String slug,
      String logoUrl,
      String publicPhone,
      String publicEmail,
      String publicContact) {}

  public record CreateMemberRequest(String fullName, String email, String phone, CompanyRole role, String password) {}

  public record UpdateMemberRequest(CompanyRole role, Membership.Status status) {}

  public record MemberResponse(
      UUID id,
      UUID userId,
      String fullName,
      String email,
      String phone,
      CompanyRole role,
      Membership.Status status) {}
}
