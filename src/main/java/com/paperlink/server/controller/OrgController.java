package com.paperlink.server.controller;

import com.paperlink.server.dtos.enums.UserRole;
import com.paperlink.server.dtos.requests.AdminLoginDto;
import com.paperlink.server.dtos.requests.OrganizationRegistrationDto;
import com.paperlink.server.dtos.response.OrganizationDto;
import com.paperlink.server.dtos.response.UserResponseDto;
import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.ErrorResponse;
import com.paperlink.server.services.JwtService;
import com.paperlink.server.services.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Organization API", description = "API endpoints for admin management")
@RequestMapping("/v1/organization")
@RequiredArgsConstructor
@Slf4j
public class OrgController {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OrganizationService organizationService;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    @Operation(
            summary = "Register a new organization",
            description = "Creates a new organization and admin user, sends verification email",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "Organization registered successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registerOrganization(@Valid @RequestBody OrganizationRegistrationDto organizationRegistrationDto) {
        OrganizationEntity organization = OrganizationEntity.builder()
                .name(organizationRegistrationDto.getOrganizationName())
                .contactEmail(organizationRegistrationDto.getAdminEmail())
                .isActive(false)
                .build();

        UserEntity adminUser = UserEntity.builder()
                .username(organizationRegistrationDto.getAdminEmail())
                .email(organizationRegistrationDto.getAdminEmail())
                .firstName(organizationRegistrationDto.getAdminFirstName())
                .lastName(organizationRegistrationDto.getAdminLastName())
                .password(passwordEncoder.encode(organizationRegistrationDto.getAdminPassword()))
                .role(UserRole.SUPER_ADMIN)
                .isVerified(false)
                .build();

        Map<String, Object> map = new HashMap<>();
        OrganizationEntity organizationEntity = organizationService.createOrganization(organization, adminUser);
        String accessToken = jwtService.generateAccessToken(adminUser);
        String refreshToken = jwtService.generateRefreshToken(adminUser);

        map.put("organization", modelMapper.map(organizationEntity, OrganizationDto.class));
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        return map;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> adminLogin(@RequestBody AdminLoginDto adminLoginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        adminLoginDto.getEmail(), adminLoginDto.getPassword())
        );
        Map<String, Object> map = new HashMap<>();
        UserEntity user = (UserEntity) authentication.getPrincipal();
        log.info("User logged in: {}", user);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        map.put("organization", modelMapper.map(user, UserResponseDto.class));
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        return map;
    }
}
