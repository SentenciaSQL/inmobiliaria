package rd.alzada.customers;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import rd.alzada.identity.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerService customers;

  public CustomerController(CustomerService customers) {
    this.customers = customers;
  }

  @GetMapping
  @PreAuthorize(SecurityRoles.STAFF)
  public PageResponse<Customer> search(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return customers.search(q, page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize(SecurityRoles.STAFF)
  public Customer get(@PathVariable UUID id) {
    return customers.get(id);
  }

  @PostMapping
  @PreAuthorize(SecurityRoles.STAFF)
  public Customer create(@Valid @RequestBody CustomerBody body) {
    return customers.create(body.toCommand());
  }

  @PutMapping("/{id}")
  @PreAuthorize(SecurityRoles.STAFF)
  public Customer update(@PathVariable UUID id, @Valid @RequestBody CustomerBody body) {
    return customers.update(id, body.toCommand());
  }

  @PostMapping("/{id}/portal")
  @PreAuthorize(SecurityRoles.ONLY_ADMIN)
  public Customer grantPortal(@PathVariable UUID id, @Valid @RequestBody PortalBody body) {
    return customers.grantPortal(id, body.password());
  }

  public record CustomerBody(
      @NotBlank @Size(max = 160) String fullName,
      @Email @Size(max = 180) String email,
      @Size(max = 30) String phone,
      @Size(max = 20) String nationalId,
      @Size(max = 240) String address) {
    CustomerService.SaveCustomer toCommand() {
      return new CustomerService.SaveCustomer(fullName, email, phone, nationalId, address);
    }
  }

  public record PortalBody(@NotBlank @PasswordPolicy String password) {}
}
