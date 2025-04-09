package com.paperlink.server.controller;

import com.paperlink.server.dtos.requests.AdminLoginDto;
import com.paperlink.server.dtos.requests.OrganizationRegistrationDto;
import com.paperlink.server.dtos.response.ApiResponse;
import com.paperlink.server.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller for organization management */
@RestController
@Tag(name = "Organization API", description = "API endpoints for organization management")
@RequestMapping("/v1/organization")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

  private final AuthService authService;

  /**
   * Register a new organization
   *
   * @param registrationDto Organization and admin registration details
   * @return New organization details and tokens
   */
  @Operation(
      summary = "Register a new organization",
      description = "Creates a new organization and admin user, sends verification email")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Organization registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ApiResponse<Map<String, Object>>> registerOrganization(
      @Valid @RequestBody OrganizationRegistrationDto registrationDto) {

    Map<String, Object> result = authService.registerOrganization(registrationDto);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Organization registered successfully", result, HttpStatus.CREATED.value()));
  }

  /**
   * Admin login
   *
   * @param loginDto Admin login credentials
   * @return User details and tokens
   */
  @Operation(
      summary = "Login as organization admin",
      description = "Authenticate as an organization admin and receive access tokens")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, Object>>> adminLogin(
      @Valid @RequestBody AdminLoginDto loginDto) {

    Map<String, Object> result = authService.loginUser(loginDto);

    return ResponseEntity.ok(
        ApiResponse.success("Login successful", result, HttpStatus.OK.value()));
  }

  /**
   * Refresh authentication tokens
   *
   * @param body Request body containing the refresh token
   * @return New tokens
   */
  @Operation(
      summary = "Refresh authentication tokens",
      description = "Get new access and refresh tokens using an existing refresh token")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tokens refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid refresh token",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping("/token/refresh")
  public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
      @RequestBody Map<String, String> body) {

    String refreshToken = body.get("refreshToken");

    // Validate input - throw exception if invalid
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new IllegalArgumentException("Refresh token is required");
    }

    // No try-catch - let the GlobalExceptionHandler handle exceptions
    Map<String, Object> result = authService.refreshTokens(refreshToken);

    return ResponseEntity.ok(
        ApiResponse.success("Tokens refreshed successfully", result, HttpStatus.OK.value()));
  }
}
