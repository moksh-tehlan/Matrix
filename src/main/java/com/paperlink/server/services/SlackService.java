package com.paperlink.server.services;

import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.SlackUserEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.repositories.SlackUserRepository;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackService {

    private final SlackWorkspaceRepository slackWorkspaceRepository;
    private final SlackUserRepository slackUserRepository;
    private final OrganizationService organizationService;

    private final Slack slack = Slack.getInstance();

    @Value("${slack.client.id}")
    private String clientId;

    @Value("${slack.client.secret}")
    private String clientSecret;

    @Value("${slack.redirect.uri}")
    private String redirectUri;

    /**
     * Complete OAuth flow and connect Slack workspace to organization
     *
     * @param code           OAuth code from Slack
     * @param organizationId Organization ID
     * @return Connected SlackWorkspaceEntity
     */
    public SlackWorkspaceEntity completeOAuthFlow(String code, String organizationId) {
        try {
            // Exchange code for tokens
            MethodsClient client = slack.methods();
            OAuthV2AccessRequest request = OAuthV2AccessRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .code(code)
                    .redirectUri(redirectUri)
                    .build();

            OAuthV2AccessResponse response = client.oauthV2Access(request);

            if (!response.isOk()) {
                log.error("Slack OAuth failed: {}", response.getError());
                throw new RuntimeException("Failed to complete Slack OAuth: " + response.getError());
            }

            // Create Slack workspace entity
            SlackWorkspaceEntity workspace = new SlackWorkspaceEntity();
            workspace.setWorkspaceId(response.getTeam().getId());
            workspace.setWorkspaceName(response.getTeam().getName());
            workspace.setAccessToken(response.getAccessToken());
            workspace.setBotToken(response.getAccessToken());
            workspace.setBotUserId(response.getBotUserId());
            workspace.setTeamName(response.getTeam().getName());


            // Save the connection
            OrganizationEntity org = organizationService.connectSlackWorkspace(organizationId, workspace);
            return org.getSlackWorkspace();

        } catch (IOException | SlackApiException e) {
            log.error("Error during Slack OAuth: ", e);
            throw new RuntimeException("Failed to complete Slack OAuth", e);
        }
    }

    /**
     * Get or create a Slack user in the system
     *
     * @param slackUserId Slack user ID
     * @param workspaceId Workspace entity ID (not Slack workspace ID)
     * @return SlackUserEntity
     */
    public SlackUserEntity getOrCreateSlackUser(String slackUserId, String workspaceId) {
        try {
            // Get workspace entity
            SlackWorkspaceEntity workspace = slackWorkspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new RuntimeException("Workspace not found"));

            // Get user info from Slack API
            MethodsClient client = slack.methods(workspace.getBotToken());
            UsersInfoRequest request = UsersInfoRequest.builder()
                    .user(slackUserId)
                    .build();

            UsersInfoResponse response = client.usersInfo(request);

            if (!response.isOk()) {
                log.error("Failed to get user info from Slack: {}", response.getError());
                throw new RuntimeException("Failed to get user info from Slack: " + response.getError());
            }

            // Create new Slack user entity
            LocalDateTime now = LocalDateTime.now();
            Optional<SlackUserEntity> slackUser = slackUserRepository.findBySlackUserIdAndWorkspaceId(slackUserId, workspaceId);
            if (slackUser.isPresent()) return slackUser.get();

            SlackUserEntity newUser = SlackUserEntity.builder()
                    .slackUserId(slackUserId)
                    .username(response.getUser().getName())
                    .workspace(workspace)
                    .organization(workspace.getOrganization())
                    .build();

            return slackUserRepository.save(newUser);

        } catch (IOException | SlackApiException e) {
            log.error("Error getting user info from Slack: ", e);
            throw new RuntimeException("Failed to get user info from Slack", e);
        }
    }

    /**
     * Send a message to a Slack user
     *
     * @param workspaceId Workspace entity ID
     * @param message     Message to send
     * @return true if message was sent successfully
     */
    public boolean sendMessage(String channelId, String workspaceId, String message) {
        try {
            // Get workspace entity
            SlackWorkspaceEntity workspace = slackWorkspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new RuntimeException("Workspace not found"));

            // Create Slack API client with bot token
            MethodsClient client = slack.methods(workspace.getBotToken());

            // Build and send message
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(channelId) // This works for both user IDs and channel IDs
                    .text(message)
                    .build();

            ChatPostMessageResponse response = client.chatPostMessage(request);

            if (!response.isOk()) {
                log.error("Failed to send message to Slack: {}", response.getError());
                return false;
            }

            return true;

        } catch (IOException | SlackApiException e) {
            log.error("Error sending message to Slack: ", e);
            return false;
        }
    }
}