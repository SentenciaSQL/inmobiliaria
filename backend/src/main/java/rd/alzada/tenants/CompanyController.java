package rd.alzada.tenants;

import rd.alzada.common.SecurityRoles;
import rd.alzada.identity.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyController {

  private final CompanyService companies;

  public CompanyController(CompanyService companies) {
    this.companies = companies;
  }

  @GetMapping
  @PreAuthorize(SecurityRoles.STAFF)
  public CompanyService.CompanyResponse current() {
    return companies.current();
  }

  @PutMapping
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CompanyService.CompanyResponse update(@Valid @RequestBody UpdateBody body) {
    return companies.update(body.toCommand());
  }

  @GetMapping("/brand")
  @PreAuthorize("isAuthenticated()")
  public CompanyService.BrandResponse brand() {
    return companies.brand();
  }

  @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CompanyService.CompanyResponse uploadLogo(@RequestParam("file") MultipartFile file) {
    return companies.uploadLogo(file);
  }

  @DeleteMapping("/logo")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CompanyService.CompanyResponse deleteLogo() {
    return companies.deleteLogo();
  }

  @GetMapping("/members")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public List<CompanyService.MemberResponse> members() {
    return companies.members();
  }

  @PostMapping("/members")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CompanyService.MemberResponse addMember(@Valid @RequestBody MemberBody body) {
    return companies.addMember(new CompanyService.CreateMemberRequest(
        body.fullName(), body.email(), body.phone(), body.role(), body.password()));
  }

  @PatchMapping("/members/{id}")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public CompanyService.MemberResponse updateMember(@PathVariable UUID id, @Valid @RequestBody MemberUpdate body) {
    return companies.updateMember(id, new CompanyService.UpdateMemberRequest(body.role(), body.status()));
  }

  public record UpdateBody(
      @NotBlank @Size(max = 160) String commercialName,
      @NotBlank @Size(max = 200) String legalName,
      @Size(max = 20) String rnc,
      @Size(max = 30) String phone,
      @Email @Size(max = 180) String email,
      @Size(max = 240) String address,
      @Size(max = 2) String provinceCode,
      UUID municipalityId,
      @Size(max = 200) String website,
      @Size(max = 4000) String description,
      @Size(max = 30) String publicPhone,
      @Email @Size(max = 180) String publicEmail,
      @Size(max = 1000) String publicContact) {

    CompanyService.UpdateCompanyRequest toCommand() {
      return new CompanyService.UpdateCompanyRequest(
          commercialName, legalName, rnc, phone, email, address, provinceCode, municipalityId, website,
          description, publicPhone, publicEmail, publicContact);
    }
  }

  public record MemberBody(
      @NotBlank @Size(max = 160) String fullName,
      @NotBlank @Email @Size(max = 180) String email,
      @Size(max = 30) String phone,
      @NotNull CompanyRole role,
      @NotBlank @PasswordPolicy String password) {}

  public record MemberUpdate(@NotNull CompanyRole role, @NotNull Membership.Status status) {}
}
