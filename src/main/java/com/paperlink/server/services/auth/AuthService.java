package com.paperlink.server.services.auth;

import com.paperlink.server.dtos.enums.UserRole;
import com.paperlink.server.dtos.requests.AdminLoginDto;
import com.paperlink.server.dtos.requests.OrganizationRegistrationDto;
import com.paperlink.server.dtos.response.UserResponseDto;
import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.AuthenticationException;
import com.paperlink.server.services.OrganizationService;
import com.paperlink.server.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for authentication and user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final ModelMapper modelMapper;

    /**
     * Register a new organization with an admin user
     *
     * @param registrationDto Organization and admin user registration details
     * @return Map containing organization, user details, and auth tokens
     */
    @Transactional
    public Map<String, Object> registerOrganization(OrganizationRegistrationDto registrationDto) {
        log.info("Registering new organization: {}", registrationDto.getOrganizationName());

        // Create organization entity
        OrganizationEntity organization = OrganizationEntity.builder()
                .name(registrationDto.getOrganizationName())
                .contactEmail(registrationDto.getAdminEmail())
                .isActive(false)
                .build();

        // Create admin user entity
        UserEntity adminUser = UserEntity.builder()
                .username(registrationDto.getAdminEmail())
                .email(registrationDto.getAdminEmail())
                .firstName(registrationDto.getAdminFirstName())
                .lastName(registrationDto.getAdminLastName())
                .password(passwordEncoder.encode(registrationDto.getAdminPassword()))
                .role(UserRole.SUPER_ADMIN)
                .isVerified(false)
                .build();

        // Register organization and admin user
        OrganizationEntity createdOrg = organizationService.createOrganization(organization, adminUser);

        // Generate tokens for the admin user
        String accessToken = jwtService.generateAccessToken(adminUser);
        String refreshToken = jwtService.generateRefreshToken(adminUser);

        // Prepare response data
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("organization", createdOrg);
        responseData.put("accessToken", accessToken);
        responseData.put("refreshToken", refreshToken);

        log.info("Successfully registered organization: {}", createdOrg.getId());
        return responseData;
    }

    /**
     * Authenticate a user with email and password
     *
     * @param loginDto Login credentials
     * @return Map containing user details and auth tokens
     * @throws AuthenticationException if authentication fails
     */
    public Map<String, Object> loginUser(AdminLoginDto loginDto) {
        log.debug("Attempting to authenticate user: {}", loginDto.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getEmail(), loginDto.getPassword())
            );

            UserEntity user = (UserEntity) authentication.getPrincipal();
            log.info("User authenticated successfully: {}", user.getUsername());

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", modelMapper.map(user, UserResponseDto.class));
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);

            return responseData;

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user: {}", loginDto.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }
    }

    /**
     * Refresh authentication tokens
     *
     * @param refreshToken Current refresh token
     * @return Map containing new access and refresh tokens
     * @throws AuthenticationException if token is invalid
     */
    public Map<String, Object> refreshTokens(String refreshToken) {
        try {
            // Validate refresh token and get user ID
            String userId = jwtService.getUserId(refreshToken);
            UserEntity user = userService.findById(userId);

            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", newAccessToken);
            responseData.put("refreshToken", newRefreshToken);

            return responseData;

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new AuthenticationException("Invalid refresh token");
        }
    }
}