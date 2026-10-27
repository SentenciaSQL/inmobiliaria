package rd.alzada.config;

import rd.alzada.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
    detail.setProperty("code", exception.code());
    return detail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail validation(MethodArgumentNotValidException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise los datos del formulario");
    detail.setProperty("code", "VALIDATION");
    Map<String, String> errors = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    detail.setProperty("errors", errors);
    return detail;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail constraint(ConstraintViolationException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise los datos enviados");
    detail.setProperty("code", "VALIDATION");
    return detail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail unreadable(HttpMessageNotReadableException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido");
    detail.setProperty("code", "BAD_REQUEST");
    return detail;
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ProblemDetail tooLarge(MaxUploadSizeExceededException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño permitido");
    detail.setProperty("code", "PAYLOAD_TOO_LARGE");
    return detail;
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail optimistic(ObjectOptimisticLockingFailureException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "El registro fue modificado por otro usuario. Actualice e intente de nuevo");
    detail.setProperty("code", "CONFLICT");
    return detail;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail integrity(DataIntegrityViolationException exception) {
    log.warn("Conflicto de integridad: {}", exception.getMostSpecificCause().getClass().getSimpleName());
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes o con una reserva simultánea");
    detail.setProperty("code", "CONFLICT");
    return detail;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail denied(AccessDeniedException exception) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación");
    detail.setProperty("code", "FORBIDDEN");
    return detail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
    log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), exception);
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación");
    detail.setProperty("code", "INTERNAL");
    return detail;
  }
}
