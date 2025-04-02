package com.paperlink.server.dtos.requests;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRegistrationDto {

    // Organization details
    @NotNull(message = "Organization name is required")
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    private String organizationName;

    // Admin user details
    @NotNull(message = "Admin first name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String adminFirstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String adminLastName;

    @NotNull(message = "Admin email is required")
    @Email(message = "Invalid email format")
    private String adminEmail;

    @NotNull(message = "Admin password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;
}