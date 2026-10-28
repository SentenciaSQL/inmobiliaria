package rd.alzada.customers;

import rd.alzada.common.SecurityRoles;
import rd.alzada.documents.DocumentController;
import rd.alzada.payments.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/buyer")
@PreAuthorize(SecurityRoles.BUYER_ONLY)
public class BuyerController {

  private final BuyerService buyers;

  public BuyerController(BuyerService buyers) {
    this.buyers = buyers;
  }

  @GetMapping("/summary")
  public BuyerService.BuyerHome summary() {
    return buyers.home();
  }

  @GetMapping("/contracts/{id}")
  public BuyerService.BuyerOperation contract(@PathVariable UUID id) {
    return buyers.operation(id);
  }

  @PostMapping(value = "/contracts/{id}/payment-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Payment report(
      @PathVariable UUID id,
      @RequestParam @NotNull BigDecimal amount,
      @RequestParam(required = false) @Size(max = 80) String reference,
      @RequestParam(required = false) @Size(max = 1000) String notes,
      @RequestParam("file") MultipartFile receipt) {
    return buyers.report(id, amount, reference, notes, receipt);
  }

  @GetMapping("/documents")
  public List<DocumentController.DocumentView> documents() {
    return buyers.documents().stream().map(DocumentController.DocumentView::from).toList();
  }
}
