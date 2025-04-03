package com.paperlink.server.services.slack;

import com.paperlink.server.entities.SlackUserEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.exceptions.SlackIntegrationException;
import com.paperlink.server.repositories.SlackUserRepository;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Slack users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackUserService {

    private final SlackUserRepository slackUserRepository;
    private final SlackWorkspaceRepository slackWorkspaceRepository;
    private final Slack slack = Slack.getInstance();

    /**
     * Get or create a Slack user in the system
     *
     * @param slackUserId Slack user ID
     * @param workspaceId Workspace entity ID (not Slack workspace ID)
     * @return SlackUserEntity
     * @throws SlackIntegrationException if user info cannot be retrieved
     */
    @Transactional
    public SlackUserEntity getOrCreateSlackUser(String slackUserId, String workspaceId) {
        try {
            // Get workspace entity
            SlackWorkspaceEntity workspace = slackWorkspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new SlackIntegrationException("Workspace not found with ID: " + workspaceId));

            // Check if user already exists
            Optional<SlackUserEntity> existingUser = slackUserRepository.findBySlackUserIdAndWorkspaceId(slackUserId, workspaceId);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            // Get user info from Slack API
            UsersInfoResponse response = getUserInfoFromSlack(slackUserId, workspace.getBotToken());

            if (!response.isOk()) {
                log.error("Failed to get user info from Slack: {}", response.getError());
                throw new SlackIntegrationException("Failed to get user info from Slack: " + response.getError());
            }

            // Create new Slack user entity
            SlackUserEntity newUser = SlackUserEntity.builder()
                    .slackUserId(slackUserId)
                    .username(response.getUser().getName())
                    .workspace(workspace)
                    .organization(workspace.getOrganization())
                    .build();

            return slackUserRepository.save(newUser);

        } catch (IOException | SlackApiException e) {
            log.error("Error getting user info from Slack: ", e);
            throw new SlackIntegrationException("Failed to get user info from Slack", e);
        }
    }

    /**
     * Get user info from Slack API
     *
     * @param slackUserId User ID in Slack
     * @param botToken    Bot token for API access
     * @return User info response
     * @throws IOException       if there's a network error
     * @throws SlackApiException if Slack API returns an error
     */
    private UsersInfoResponse getUserInfoFromSlack(String slackUserId, String botToken) throws IOException, SlackApiException {
        MethodsClient client = slack.methods(botToken);
        UsersInfoRequest request = UsersInfoRequest.builder()
                .user(slackUserId)
                .build();

        return client.usersInfo(request);
    }

    /**
     * Find all users in a workspace
     *
     * @param workspaceId Workspace ID
     * @return List of Slack users
     */
    @Transactional(readOnly = true)
    public List<SlackUserEntity> findUsersByWorkspace(String workspaceId) {
        return slackUserRepository.findByWorkspaceId(workspaceId);
    }

    /**
     * Find all users in an organization
     *
     * @param organizationId Organization ID
     * @return List of Slack users
     */
    @Transactional(readOnly = true)
    public List<SlackUserEntity> findUsersByOrganization(String organizationId) {
        return slackUserRepository.findByOrganizationId(organizationId);
    }
}