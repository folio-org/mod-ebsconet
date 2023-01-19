package org.folio.ebsconet.error;

import feign.FeignException;
import feign.FeignException.BadRequest;
import feign.FeignException.InternalServerError;
import lombok.extern.log4j.Log4j2;
import org.folio.ebsconet.domain.dto.Error;
import org.folio.ebsconet.domain.dto.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.ConstraintViolationException;

import static org.folio.ebsconet.error.ErrorCode.INTERNAL_SERVER_ERROR;
import static org.folio.ebsconet.error.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.ebsconet.error.ErrorCode.UNKNOWN_ERROR;
import static org.folio.ebsconet.error.ErrorCode.VALIDATION_ERROR;
import static org.folio.ebsconet.error.ErrorType.INTERNAL;
import static org.folio.ebsconet.error.ErrorType.UNKNOWN;

@ControllerAdvice
@Log4j2
public class DefaultErrorHandler {

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Errors> handleConstraintViolation(final ConstraintViolationException exception) {
    log.error("DefaultErrorHandler:: ConstraintViolationException: " + exception.getMessage());
    var errors = new Errors();
    exception.getConstraintViolations().forEach(constraintViolation ->
      errors.addErrorsItem(new Error()
        .message(constraintViolation.getMessage())
        .code(VALIDATION_ERROR.getDescription())
        .type(INTERNAL.getValue())));
    errors.setTotalRecords(errors.getErrors().size());
    return ResponseEntity
      .badRequest()
      .body(errors);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Errors> handleNotFoundExceptions(final ResourceNotFoundException exception) {
    log.error("DefaultErrorHandler:: ResourceNotFoundException: " + exception.getMessage());
    var errors = new Errors();
    errors.addErrorsItem(new Error()
      .message(exception.getMessage())
      .code(NOT_FOUND_ERROR.getDescription())
      .type(INTERNAL.getValue()));
    errors.setTotalRecords(1);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(errors);
  }

  @ExceptionHandler(InternalServerError.class)
  public ResponseEntity<Errors> handleInternalServerError(final InternalServerError exception) {
    log.error("DefaultErrorHandler:: InternalServerError: " + exception.getMessage());
    var errors = new Errors();
    errors.addErrorsItem(new Error()
      .message(exception.getMessage())
      .code(INTERNAL_SERVER_ERROR.getDescription())
      .type(INTERNAL.getValue()));
    errors.setTotalRecords(1);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(errors);
  }

  @ExceptionHandler({ UnprocessableEntity.class, FeignException.UnprocessableEntity.class })
  public ResponseEntity<Errors> handleUnprocessableEntityError(final Exception exception) {
    log.error("DefaultErrorHandler:: UnprocessableEntity: " + exception.getMessage());
    var errors = new Errors();
    errors.addErrorsItem(new Error()
      .message(exception.getMessage())
      .type(INTERNAL.getValue()));
    errors.setTotalRecords(1);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
      .body(errors);
  }

  @ExceptionHandler(BadRequest.class)
  public ResponseEntity<Errors> handleBadRequestEntityError(final BadRequest exception) {
    log.error("DefaultErrorHandler:: BadRequest: " + exception.getMessage());
    var errors = new Errors();
    errors.addErrorsItem(new Error()
      .message(exception.getMessage())
      .type(INTERNAL.getValue()));
    errors.setTotalRecords(1);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
      .body(errors);
  }

  @ExceptionHandler({ NullPointerException.class, IllegalArgumentException.class, IllegalStateException.class })
  public ResponseEntity<Errors> handleInternal(final RuntimeException exception) {
    log.error("DefaultErrorHandler:: " + exception.getClass().getName() + ": " + exception.getMessage());
    return buildUnknownErrorResponse(exception.getMessage());
  }

  private ResponseEntity<Errors> buildUnknownErrorResponse(String message) {
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(new Errors()
        .addErrorsItem(new Error()
          .message(message)
          .code(UNKNOWN_ERROR.getDescription())
          .type(UNKNOWN.getValue()))
        .totalRecords(1));
  }
}
