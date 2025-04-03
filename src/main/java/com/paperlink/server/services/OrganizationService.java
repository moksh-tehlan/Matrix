package com.paperlink.server.services;

import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.DuplicateResourceException;
import com.paperlink.server.exceptions.ResourceNotFoundException;
import com.paperlink.server.repositories.OrganizationRepository;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.paperlink.server.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing organizations
 */
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

        // Format subdomain
        String formattedSubdomain = StringUtils.formatSubdomain(organization.getName());
        organization.setSubdomain(formattedSubdomain);

        // Save the organization first
        OrganizationEntity savedOrg = organizationRepository.save(organization);

        // Set organization reference on user and save
        adminUser.setOrganization(savedOrg);
        userService.createUser(adminUser);

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
     * @throws DuplicateResourceException if workspace is already connected to another organization
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

        // Set organization reference on workspace
        slackWorkspace.setOrganization(organization);

        // Save workspace and update organization reference
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
    @Transactional(readOnly = true)
    public OrganizationEntity findById(String id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
    }

    /**
     * Find all organizations
     *
     * @return List of all organizations
     */
    @Transactional(readOnly = true)
    public List<OrganizationEntity> findAll() {
        return organizationRepository.findAll();
    }

    /**
     * Update an organization
     *
     * @param id Organization ID
     * @param organizationDetails Updated organization details
     * @return The updated organization
     * @throws ResourceNotFoundException if organization is not found
     */
    @Transactional
    public OrganizationEntity updateOrganization(String id, OrganizationEntity organizationDetails) {
        OrganizationEntity organization = findById(id);

        // Update fields
        organization.setName(organizationDetails.getName());
        organization.setContactEmail(organizationDetails.getContactEmail());
        organization.setDescription(organizationDetails.getDescription());
        organization.setActive(organizationDetails.isActive());

        return organizationRepository.save(organization);
    }

    /**
     * Activate or deactivate an organization
     *
     * @param id Organization ID
     * @param active Active status
     * @return The updated organization
     * @throws ResourceNotFoundException if organization is not found
     */
    @Transactional
    public OrganizationEntity setActiveStatus(String id, boolean active) {
        OrganizationEntity organization = findById(id);
        organization.setActive(active);
        return organizationRepository.save(organization);
    }

    /**
     * Delete an organization
     *
     * @param id Organization ID
     * @throws ResourceNotFoundException if organization is not found
     */
    @Transactional
    public void deleteOrganization(String id) {
        OrganizationEntity organization = findById(id);
        organizationRepository.delete(organization);
        log.info("Organization deleted: {}", id);
    }
}