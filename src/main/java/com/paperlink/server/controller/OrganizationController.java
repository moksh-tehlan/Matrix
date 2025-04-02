package com.paperlink.server.controller;

import com.paperlink.server.dtos.requests.OrganizationRegistrationDto;
import com.paperlink.server.dtos.response.OrganizationDto;
import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.ErrorResponse;
import com.paperlink.server.services.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1")
@Tag(name = "Organization API", description = "API endpoints for organization management")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;
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
    public OrganizationDto registerOrganization(@Valid @RequestBody OrganizationRegistrationDto organizationRegistrationDto) {
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
                .isVerified(false)
                .build();

        OrganizationEntity organizationEntity = organizationService.createOrganization(organization, adminUser);
        return modelMapper.map(organizationEntity, OrganizationDto.class);
    }
}
