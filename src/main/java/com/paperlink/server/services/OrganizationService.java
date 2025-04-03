package com.paperlink.server.services;

import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.DuplicateResourceException;
import com.paperlink.server.exceptions.ResourceNotFoundException;
import com.paperlink.server.repositories.OrganizationRepository;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
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
    private final SlackWorkspaceRepository slackWorkspaceRepository;
    private final UserService userService;

    /**
     * Create a new organization
     *
     * @param organization The organization entity to create
     * @param adminUser    The admin user for the organization
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
     * Connect a Slack workspace to an organization
     *
     * @param organizationId Organization ID
     * @param slackWorkspace Slack workspace details
     * @return Updated organization
     * @throws ResourceNotFoundException if organization is not found
     */
    @Transactional
    public OrganizationEntity connectSlackWorkspace(String organizationId, SlackWorkspaceEntity slackWorkspace) {
        OrganizationEntity organization = findById(organizationId);

        // Check if this Slack workspace is already connected to another organization
        slackWorkspaceRepository.findByWorkspaceId(slackWorkspace.getWorkspaceId())
                .ifPresent(existing -> {
                    if (!existing.getOrganization().getId().equals(organizationId)) {
                        throw new DuplicateResourceException("This Slack workspace is already connected to another organization");
                    }
                });

        slackWorkspace.setOrganization(organization);

        SlackWorkspaceEntity savedWorkspace = slackWorkspaceRepository.save(slackWorkspace);
        organization.setSlackWorkspace(savedWorkspace);

        log.info("Slack workspace connected to organization: {}", organization.getName());
        return organizationRepository.save(organization);
    }

    /**
     * Find organization by ID
     *
     * @param id Organization ID
     * @return The organization entity
     * @throws ResourceNotFoundException if organization is not found
     */
    public OrganizationEntity findById(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
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
