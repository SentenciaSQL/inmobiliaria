package rd.alzada.payments;

import rd.alzada.common.PageResponse;
import rd.alzada.common.SecurityRoles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FinanceController {

  private final FinanceService finance;

  public FinanceController(FinanceService finance) {
    this.finance = finance;
  }

  @GetMapping("/contracts/{contractId}/payment-plan")
  @PreAuthorize(SecurityRoles.CONTRACT_READ)
  public FinanceService.PlanView plan(@PathVariable UUID contractId) {
    return finance.plan(contractId);
  }

  @PutMapping("/contracts/{contractId}/payment-plan")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public FinanceService.PlanView savePlan(@PathVariable UUID contractId, @Valid @RequestBody PlanBody body) {
    return finance.savePlan(contractId, body.toCommand());
  }

  @GetMapping("/payments")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public PageResponse<Payment> payments(
      @RequestParam(required = false) Payment.Status status,
      @RequestParam(required = false) UUID contractId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return finance.search(status, contractId, page, size);
  }

  @PostMapping(value = "/contracts/{contractId}/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Payment register(
      @PathVariable UUID contractId,
      @RequestParam BigDecimal amount,
      @RequestParam(required = false) String reference,
      @RequestParam(required = false) String notes,
      @RequestPart(required = false) List<FinanceService.AllocationCommand> allocations,
      @RequestPart(required = false) MultipartFile receipt) {
    return finance.register(contractId, amount, reference, notes, allocations == null ? List.of() : allocations, receipt);
  }

  @PostMapping("/payments/{id}/validate")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Payment validate(@PathVariable UUID id, @RequestBody(required = false) AllocationBody body) {
    return finance.validate(id, body == null || body.allocations() == null ? List.of() : body.allocations());
  }

  @PostMapping("/payments/{id}/reject")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Payment reject(@PathVariable UUID id, @Valid @RequestBody ReasonBody body) {
    return finance.reject(id, body.reason());
  }

  @PostMapping("/payments/{id}/reverse")
  @PreAuthorize(SecurityRoles.FINANCE_ACCESS)
  public Payment reverse(@PathVariable UUID id, @Valid @RequestBody ReasonBody body) {
    return finance.reverse(id, body.reason());
  }

  public record PlanBody(
      @NotNull BigDecimal totalPrice,
      BigDecimal downPayment,
      LocalDate downPaymentDueDate,
      int installmentCount,
      @NotNull PaymentPlan.Frequency frequency,
      LocalDate firstDueDate,
      BigDecimal deliveryAmount,
      LocalDate deliveryDueDate,
      List<ExtraBody> extraordinary,
      @Size(max = 4000) String conditions) {

    FinanceService.PlanCommand toCommand() {
      List<FinanceService.PlanCommand.Extra> extras = extraordinary == null ? List.of() : extraordinary.stream()
          .map(extra -> new FinanceService.PlanCommand.Extra(extra.label(), extra.dueDate(), extra.amount()))
          .toList();
      return new FinanceService.PlanCommand(
          totalPrice, downPayment, downPaymentDueDate, installmentCount, frequency, firstDueDate, deliveryAmount, deliveryDueDate, extras, conditions);
    }
  }

  public record ExtraBody(@NotBlank @Size(max = 120) String label, @NotNull LocalDate dueDate, @NotNull BigDecimal amount) {}

  public record AllocationBody(List<FinanceService.AllocationCommand> allocations) {}

  public record ReasonBody(@NotBlank @Size(max = 400) String reason) {}
}
