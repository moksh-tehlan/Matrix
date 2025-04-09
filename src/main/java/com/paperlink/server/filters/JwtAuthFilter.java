package com.paperlink.server.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperlink.server.dtos.response.ApiResponse;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.AuthenticationException;
import com.paperlink.server.services.UserService;
import com.paperlink.server.services.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Filter for JWT authentication */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
  private final UserService userService;
  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // Extract token
      final String token = authHeader.substring(7);

      // Make sure it's an access token
      if (!jwtService.isAccessToken(token)) {
        throw new AuthenticationException("Not an access token");
      }

      // Get user ID from token and find user
      final String userId = jwtService.getUserId(token);

      if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserEntity user = userService.findById(userId);

        if (jwtService.validateToken(token)) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);

          log.debug("Authenticated user: {}", user.getUsername());
        }
      }

      filterChain.doFilter(request, response);

    } catch (ExpiredJwtException e) {
      log.debug("Expired JWT token");
      handleAuthenticationException(response, "Token expired", HttpStatus.UNAUTHORIZED);
    } catch (MalformedJwtException e) {
      log.debug("Invalid JWT token");
      handleAuthenticationException(response, "Invalid token", HttpStatus.UNAUTHORIZED);
    } catch (AccessDeniedException e) {
      log.warn("Access denied: {}", e.getMessage());
      handleAuthenticationException(response, "Access denied", HttpStatus.FORBIDDEN);
    } catch (AuthenticationException e) {
      log.warn("Authentication error: {}", e.getMessage());
      handleAuthenticationException(response, e.getMessage(), HttpStatus.UNAUTHORIZED);
    } catch (Exception e) {
      log.error("Authentication error: {}", e.getMessage(), e);
      handleAuthenticationException(response, "Authentication error", HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Handle authentication exceptions by returning proper error response
   *
   * @param response HttpServletResponse
   * @param message Error message
   * @param status HTTP status
   * @throws IOException if response cannot be written
   */
  private void handleAuthenticationException(
      HttpServletResponse response, String message, HttpStatus status) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiResponse<Object> errorResponse = ApiResponse.error(message, status.value(), message, null);

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
