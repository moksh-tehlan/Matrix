package com.paperlink.server.services.slack;

import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.exceptions.SlackIntegrationException;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Service for sending messages to Slack */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackMessageService {

  private final SlackWorkspaceRepository slackWorkspaceRepository;
  private final Slack slack = Slack.getInstance();

  /**
   * Send a message to a Slack channel or user
   *
   * @param channelId Channel ID or user ID
   * @param workspaceId Workspace entity ID
   * @param message Message to send
   * @return true if message was sent successfully
   * @throws SlackIntegrationException if message sending fails
   */
  @Retryable(
      retryFor = {SlackIntegrationException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  public boolean sendMessage(String channelId, String workspaceId, String message) {
    try {
      // Get workspace entity
      SlackWorkspaceEntity workspace =
          slackWorkspaceRepository
              .findById(workspaceId)
              .orElseThrow(
                  () ->
                      new SlackIntegrationException("Workspace not found with ID: " + workspaceId));

      // Send message
      ChatPostMessageResponse response =
          sendSlackMessage(channelId, message, workspace.getBotToken());

      if (!response.isOk()) {
        log.error("Failed to send message to Slack: {}", response.getError());
        throw new SlackIntegrationException(
            "Failed to send message to Slack: " + response.getError());
      }

      log.debug("Message sent successfully to channel/user: {}", channelId);
      return true;

    } catch (IOException | SlackApiException e) {
      log.error("Error sending message to Slack: ", e);
      throw new SlackIntegrationException("Error sending message to Slack", e);
    }
  }

  /**
   * Send a message to multiple channels
   *
   * @param channelIds List of channel IDs
   * @param workspaceId Workspace entity ID
   * @param message Message to send
   * @return Number of channels message was successfully sent to
   */
  public int sendBroadcastMessage(Iterable<String> channelIds, String workspaceId, String message) {
    int successCount = 0;

    for (String channelId : channelIds) {
      try {
        if (sendMessage(channelId, workspaceId, message)) {
          successCount++;
        }
      } catch (SlackIntegrationException e) {
        log.warn("Failed to send broadcast message to channel {}: {}", channelId, e.getMessage());
        // Continue with other channels
      }
    }

    return successCount;
  }

  /**
   * Send a message using Slack API
   *
   * @param channelId Channel ID or user ID
   * @param message Message text
   * @param botToken Bot token for API access
   * @return Response from Slack API
   * @throws IOException if there's a network error
   * @throws SlackApiException if Slack API returns an error
   */
  private ChatPostMessageResponse sendSlackMessage(
      String channelId, String message, String botToken) throws IOException, SlackApiException {

    MethodsClient client = slack.methods(botToken);

    ChatPostMessageRequest request =
        ChatPostMessageRequest.builder().channel(channelId).text(message).build();

    return client.chatPostMessage(request);
  }
}
