package rd.alzada.customers;

import rd.alzada.common.ApiException;
import rd.alzada.common.PageResponse;
import rd.alzada.common.Pages;
import rd.alzada.common.SecuritySupport;
import rd.alzada.common.Texts;
import rd.alzada.identity.User;
import rd.alzada.identity.UserRepository;
import rd.alzada.tenants.CompanyRole;
import rd.alzada.tenants.Membership;
import rd.alzada.tenants.MembershipRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository customers;
  private final UserRepository users;
  private final MembershipRepository memberships;
  private final PasswordEncoder passwordEncoder;

  public CustomerService(
      CustomerRepository customers,
      UserRepository users,
      MembershipRepository memberships,
      PasswordEncoder passwordEncoder) {
    this.customers = customers;
    this.users = users;
    this.memberships = memberships;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public PageResponse<Customer> search(String query, int page, int size) {
    return PageResponse.of(customers.search(SecuritySupport.tenantId(), Texts.like(query), Pages.desc(page, size)));
  }

  @Transactional(readOnly = true)
  public Customer get(UUID id) {
    return require(id);
  }

  @Transactional
  public Customer create(SaveCustomer request) {
    Customer customer = new Customer();
    customer.setTenantId(SecuritySupport.tenantId());
    apply(customer, request);
    return customers.save(customer);
  }

  @Transactional
  public Customer update(UUID id, SaveCustomer request) {
    Customer customer = require(id);
    apply(customer, request);
    return customer;
  }

  @Transactional
  public Customer grantPortal(UUID id, String password) {
    Customer customer = require(id);
    if (customer.getEmail() == null) {
      throw ApiException.badRequest("El comprador necesita un correo para acceder al portal");
    }
    UUID tenantId = customer.getTenantId();
    User user = customer.getUserId() == null ? users.findByEmailIgnoreCase(customer.getEmail()).orElse(null) : users.findById(customer.getUserId()).orElse(null);
    if (user == null) {
      if (password == null || password.length() < 10) {
        throw ApiException.badRequest("Indique una contraseña inicial de al menos 10 caracteres");
      }
      user = new User();
      user.setEmail(customer.getEmail());
      user.setFullName(customer.getFullName());
      user.setPhone(customer.getPhone());
      user.setPasswordHash(passwordEncoder.encode(password));
      user.setStatus(User.Status.ACTIVE);
      users.save(user);
    }
    customer.setUserId(user.getId());
    if (!memberships.existsByUserIdAndCompanyIdAndRoleAndStatus(user.getId(), tenantId, CompanyRole.BUYER, Membership.Status.ACTIVE)) {
      Membership membership = new Membership();
      membership.setUserId(user.getId());
      membership.setCompanyId(tenantId);
      membership.setRole(CompanyRole.BUYER);
      membership.setStatus(Membership.Status.ACTIVE);
      memberships.save(membership);
    }
    return customer;
  }

  private Customer require(UUID id) {
    return customers.findByIdAndTenantId(id, SecuritySupport.tenantId())
        .orElseThrow(() -> ApiException.notFound("El comprador no existe"));
  }

  private static void apply(Customer customer, SaveCustomer request) {
    customer.setFullName(request.fullName().trim());
    customer.setEmail(Texts.lowerEmail(request.email()));
    customer.setPhone(Texts.blankToNull(request.phone()));
    customer.setNationalId(Texts.blankToNull(request.nationalId()));
    customer.setAddress(Texts.blankToNull(request.address()));
  }

  public record SaveCustomer(String fullName, String email, String phone, String nationalId, String address) {}
}
