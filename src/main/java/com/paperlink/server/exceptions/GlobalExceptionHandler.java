package com.paperlink.server.exceptions;

import com.paperlink.server.dtos.response.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.core.exception.SdkException;

/** Global exception handler for centralized API error handling */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /** Handle custom AuthenticationException */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
      AuthenticationException ex, WebRequest request) {

    log.warn("Authentication exception: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ApiResponse.error(
                "Authentication failed",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Handle custom SlackIntegrationException */
  @ExceptionHandler(SlackIntegrationException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ApiResponse<Object>> handleSlackIntegrationException(
      SlackIntegrationException ex, WebRequest request) {

    log.error("Slack integration error: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                "Slack integration failed",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Handle ResourceNotFoundException */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request) {

    log.warn("Resource not found: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiResponse.error(
                "Resource not found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Handle DuplicateResourceException */
  @ExceptionHandler(DuplicateResourceException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(
      DuplicateResourceException ex, WebRequest request) {

    log.warn("Duplicate resource: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiResponse.error(
                "Resource already exists",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Handle DocumentProcessingException */
  @ExceptionHandler(DocumentProcessingException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ApiResponse<Object>> handleDocumentProcessingException(
      DocumentProcessingException ex, WebRequest request) {

    log.error("Document processing error: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                "Document processing failed",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Handle Bean validation errors (when @Valid fails) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {

    // Create a map of field name to error message
    Map<String, String> validationErrors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              validationErrors.put(fieldName, errorMessage);
            });

    log.debug("Validation failed with {} errors: {}", validationErrors.size(), validationErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.error(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                "The request contains invalid parameters",
                request.getDescription(false),
                validationErrors));
  }

  /** Handle file size limit exceeded */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ResponseEntity<ApiResponse<Object>> handleMaxSizeException(
      MaxUploadSizeExceededException ex, WebRequest request) {

    log.warn("File size limit exceeded: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ApiResponse.error(
                "File too large",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "The uploaded file exceeds the maximum allowed size",
                request.getDescription(false)));
  }

  /** Handle AWS SDK exceptions */
  @ExceptionHandler(SdkException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ApiResponse<Object>> handleAwsSdkException(
      SdkException ex, WebRequest request) {

    log.error("AWS SDK error: {}", ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                "AWS service error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An error occurred with an AWS service",
                request.getDescription(false)));
  }

  /** Handle IllegalArgumentException */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    log.warn("Invalid argument: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.error(
                "Invalid request",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getDescription(false)));
  }

  /** Fallback handler for all other exceptions */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ApiResponse<Object>> handleGlobalException(
      Exception ex, WebRequest request) {

    log.error("Unhandled exception", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later or contact support.",
                request.getDescription(false)));
  }
}
