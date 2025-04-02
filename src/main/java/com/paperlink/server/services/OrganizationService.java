package com.paperlink.server.services;

import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.repositories.OrganizationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserService userService;

    /**
     * Create a new organization
     *
     * @param organization The organization entity to create
     * @param adminUser The admin user for the organization
     * @return The created organization entity
     */
    @Transactional
    public OrganizationEntity createOrganization(OrganizationEntity organization, UserEntity adminUser) {
        log.info("Creating new organization: {}", organization.getName());

        // Generate verification token
        organization.setVerificationToken(UUID.randomUUID().toString());
        organization.setActive(true);

        // Format subdomain (lowercase, remove spaces, etc.)
        String formattedSubdomain = formatSubdomain(organization.getName());
        organization.setSubdomain(formattedSubdomain);

        // Save the admin
        adminUser.setVerified(true);
        userService.createUser(adminUser);
        // Save the organization
        OrganizationEntity savedOrg = organizationRepository.save(organization);

        log.info("Organization created successfully with ID: {}", savedOrg.getId());
        return savedOrg;
    }

    /**
     * Format a string to be used as a subdomain
     *
     * @param name Name to format
     * @return Formatted subdomain
     */
    private String formatSubdomain(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("\\s+", "");
    }

}
