package rd.alzada.contracts;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

  private final ContractService contracts;

  public ContractController(ContractService contracts) {
    this.contracts = contracts;
  }

  @GetMapping
  @PreAuthorize(SecurityRoles.CONTRACT_READ)
  public PageResponse<Contract> search(
      @RequestParam(required = false) Contract.Status status,
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return contracts.search(status, projectId, page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize(SecurityRoles.CONTRACT_READ)
  public ContractService.ContractDetail get(@PathVariable UUID id) {
    return contracts.get(id);
  }

  @PostMapping
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Contract create(@Valid @RequestBody ContractBody body) {
    return contracts.create(body.toCommand());
  }

  @PutMapping("/{id}")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Contract update(@PathVariable UUID id, @Valid @RequestBody ContractBody body) {
    return contracts.update(id, body.toCommand());
  }

  @PostMapping("/{id}/parties")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public ContractParty addParty(@PathVariable UUID id, @Valid @RequestBody PartyBody body) {
    return contracts.addParty(id, body.customerId(), body.primary());
  }

  @DeleteMapping("/{id}/parties/{customerId}")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public void removeParty(@PathVariable UUID id, @PathVariable UUID customerId) {
    contracts.removeParty(id, customerId);
  }

  @PostMapping("/{id}/activate")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Contract activate(@PathVariable UUID id) {
    return contracts.activate(id);
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Contract complete(@PathVariable UUID id) {
    return contracts.complete(id);
  }

  @PostMapping("/{id}/annul")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Contract annul(@PathVariable UUID id, @Valid @RequestBody AnnulBody body) {
    return contracts.annul(id, body.reason());
  }

  public record ContractBody(
      @NotNull UUID projectId,
      @NotNull UUID unitId,
      UUID customerId,
      @NotNull BigDecimal agreedPrice,
      @Size(max = 3) String currency,
      LocalDate agreementDate,
      BigDecimal agreedDownPayment,
      @Size(max = 4000) String notes) {

    ContractService.CreateContract toCommand() {
      return new ContractService.CreateContract(projectId, unitId, customerId, agreedPrice, agreedDownPayment, currency, agreementDate, notes);
    }
  }

  public record PartyBody(@NotNull UUID customerId, boolean primary) {}

  public record AnnulBody(@NotBlank @Size(max = 400) String reason) {}
}
