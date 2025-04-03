package com.paperlink.server.services.slack;

import com.paperlink.server.entities.OrganizationEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.exceptions.SlackIntegrationException;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.paperlink.server.services.OrganizationService;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Service for handling Slack authentication and OAuth flows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackAuthService {

    private final SlackWorkspaceRepository slackWorkspaceRepository;
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
     * @throws SlackIntegrationException if the OAuth flow fails
     */
    @Transactional
    public SlackWorkspaceEntity completeOAuthFlow(String code, String organizationId) {
        try {
            // Exchange code for tokens
            OAuthV2AccessResponse response = exchangeCodeForTokens(code);

            if (!response.isOk()) {
                log.error("Slack OAuth failed: {}", response.getError());
                throw new SlackIntegrationException("Failed to complete Slack OAuth: " + response.getError());
            }

            // Create Slack workspace entity
            SlackWorkspaceEntity workspace = createWorkspaceFromResponse(response);

            // Connect to organization and save
            OrganizationEntity org = organizationService.connectSlackWorkspace(organizationId, workspace);
            return org.getSlackWorkspace();

        } catch (IOException | SlackApiException e) {
            log.error("Error during Slack OAuth: ", e);
            throw new SlackIntegrationException("Failed to complete Slack OAuth", e);
        }
    }

    /**
     * Exchange OAuth code for access tokens
     *
     * @param code OAuth code from Slack
     * @return OAuth response containing tokens
     * @throws IOException       if there's a network error
     * @throws SlackApiException if Slack API returns an error
     */
    private OAuthV2AccessResponse exchangeCodeForTokens(String code) throws IOException, SlackApiException {
        MethodsClient client = slack.methods();
        OAuthV2AccessRequest request = OAuthV2AccessRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .code(code)
                .redirectUri(redirectUri)
                .build();

        return client.oauthV2Access(request);
    }

    /**
     * Create a workspace entity from OAuth response
     *
     * @param response OAuth response containing workspace details
     * @return SlackWorkspaceEntity
     */
    private SlackWorkspaceEntity createWorkspaceFromResponse(OAuthV2AccessResponse response) {
        SlackWorkspaceEntity workspace = new SlackWorkspaceEntity();
        workspace.setWorkspaceId(response.getTeam().getId());
        workspace.setWorkspaceName(response.getTeam().getName());
        workspace.setAccessToken(response.getAccessToken());
        workspace.setBotToken(response.getAccessToken());
        workspace.setBotUserId(response.getBotUserId());
        workspace.setTeamName(response.getTeam().getName());
        return workspace;
    }
}