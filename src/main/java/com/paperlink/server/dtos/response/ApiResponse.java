package com.paperlink.server.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for consistent API output
 *
 * @param <T> The type of data contained in the response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  // Success flag
  private boolean success;

  // Status message
  private String message;

  // Response data
  private T data;

  // HTTP status code
  private int status;

  // Timestamp of the response
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp = LocalDateTime.now(ZoneOffset.UTC);

  // Error details, included only in error responses
  private String error;

  // Path that triggered the error
  private String path;

  // Validation errors, included only when validation fails
  private Map<String, String> validationErrors;

  /**
   * Constructor for success responses
   *
   * @param success Success flag
   * @param message Status message
   * @param data Response data
   * @param status HTTP status code
   */
  public ApiResponse(boolean success, String message, T data, int status) {
    this.success = success;
    this.message = message;
    this.data = data;
    this.status = status;
    this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Constructor for error responses
   *
   * @param success Success flag
   * @param message Status message
   * @param status HTTP status code
   * @param error Error details
   * @param path Path that triggered the error
   */
  public ApiResponse(boolean success, String message, int status, String error, String path) {
    this.success = success;
    this.message = message;
    this.status = status;
    this.error = error;
    this.path = path;
    this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Constructor for validation error responses
   *
   * @param success Success flag
   * @param message Status message
   * @param status HTTP status code
   * @param error Error details
   * @param path Path that triggered the error
   * @param validationErrors Map of field names to error messages
   */
  public ApiResponse(
      boolean success,
      String message,
      int status,
      String error,
      String path,
      Map<String, String> validationErrors) {
    this.success = success;
    this.message = message;
    this.status = status;
    this.error = error;
    this.path = path;
    this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    this.validationErrors = validationErrors;
  }

  /**
   * Static factory method for success responses
   *
   * @param message Status message
   * @param data Response data
   * @param status HTTP status code
   * @return ApiResponse instance
   * @param <T> Type of data
   */
  public static <T> ApiResponse<T> success(String message, T data, int status) {
    return new ApiResponse<>(true, message, data, status);
  }

  /**
   * Static factory method for error responses
   *
   * @param message Status message
   * @param status HTTP status code
   * @param error Error details
   * @param path Path that triggered the error
   * @return ApiResponse instance
   * @param <T> Type of data
   */
  public static <T> ApiResponse<T> error(String message, int status, String error, String path) {
    return new ApiResponse<>(false, message, status, error, path);
  }

  /**
   * Static factory method for validation error responses
   *
   * @param message Status message
   * @param status HTTP status code
   * @param error Error details
   * @param path Path that triggered the error
   * @param validationErrors Map of field names to error messages
   * @return ApiResponse instance
   * @param <T> Type of data
   */
  public static <T> ApiResponse<T> error(
      String message, int status, String error, String path, Map<String, String> validationErrors) {
    return new ApiResponse<>(false, message, status, error, path, validationErrors);
  }
}
