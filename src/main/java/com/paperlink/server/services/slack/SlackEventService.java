package com.paperlink.server.services.slack;

import com.paperlink.server.entities.SlackUserEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.exceptions.SlackIntegrationException;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.paperlink.server.services.vector.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for processing Slack events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackEventService {

    private final SlackWorkspaceRepository slackWorkspaceRepository;
    private final SlackUserService slackUserService;
    private final SlackMessageService slackMessageService;
    private final VectorService vectorService;

    /**
     * Process a message event from Slack
     *
     * @param payload Full event payload
     * @param event   Event details
     * @return true if the event was processed successfully
     */
    public boolean processMessageEvent(Map<String, Object> payload, Map<String, Object> event) {
        log.debug("Processing Slack message event: {}", event);

        try {
            // Extract relevant information
            String teamId = (String) payload.get("team_id");
            String userId = (String) event.get("user");
            String text = (String) event.get("text");
            String channel = (String) event.get("channel");
            String eventType = (String) event.get("type");

            // Skip bot messages to prevent loops
            if (event.containsKey("bot_id") || event.containsKey("bot_profile")) {
                log.debug("Skipping bot message");
                return false;
            }

            // Determine if we should process this message
            if (!shouldProcessMessage(channel, eventType, text, getBotUserId(payload))) {
                log.debug("Message doesn't require processing");
                return false;
            }

            // Clean the text (remove mentions)
            text = cleanMessageText(text, getBotUserId(payload));
            log.info("Processing message from user: {}, text: {}", userId, text);

            // Find workspace
            Optional<SlackWorkspaceEntity> workspaceOpt = slackWorkspaceRepository.findByWorkspaceId(teamId);
            if (workspaceOpt.isEmpty()) {
                log.error("Workspace not found for team ID: {}", teamId);
                return false;
            }

            SlackWorkspaceEntity workspace = workspaceOpt.get();

            // Process user and generate response
            SlackUserEntity slackUser = slackUserService.getOrCreateSlackUser(userId, workspace.getId());

            // Use vector service to get response
            String response = vectorService.getResponse(text, slackUser.getId());

            // Send response back to channel
            slackMessageService.sendMessage(channel, workspace.getId(), response);
            return true;

        } catch (Exception e) {
            log.error("Error processing message event: ", e);
            throw new SlackIntegrationException("Failed to process message event", e);
        }
    }

    /**
     * Process a slash command from Slack
     *
     * @param payload Command payload
     * @return Response text for the command
     */
    public Map<String, Object> processSlashCommand(Map<String, String> payload) {
        String command = payload.get("command");
        String text = payload.get("text");
        String userId = payload.get("user_id");
        String teamId = payload.get("team_id");
        String channelId = payload.get("channel_id");

        log.info("Processing slash command: {}, text: {}, user: {}", command, text, userId);

        try {
            // Handle different commands
            switch (command) {
                case "/paperlink-search":
                    return handleSearchCommand(text, userId, teamId);

                case "/paperlink-help":
                    return handleHelpCommand();

                default:
                    return Map.of(
                            "response_type", "ephemeral",
                            "text", "Unknown command: " + command
                    );
            }
        } catch (Exception e) {
            log.error("Error processing slash command: ", e);
            return Map.of(
                    "response_type", "ephemeral",
                    "text", "An error occurred processing your command: " + e.getMessage()
            );
        }
    }

    /**
     * Handle the search command
     *
     * @param query  Search query
     * @param userId User ID
     * @param teamId Team ID
     * @return Response for the command
     */
    private Map<String, Object> handleSearchCommand(String query, String userId, String teamId) {
        Optional<SlackWorkspaceEntity> workspaceOpt = slackWorkspaceRepository.findByWorkspaceId(teamId);

        if (workspaceOpt.isEmpty()) {
            return Map.of(
                    "response_type", "ephemeral",
                    "text", "Error: This workspace is not properly connected to PaperLink"
            );
        }

        SlackWorkspaceEntity workspace = workspaceOpt.get();
        SlackUserEntity slackUser = slackUserService.getOrCreateSlackUser(userId, workspace.getId());
        String response = vectorService.getResponse(query, slackUser.getId());

        return Map.of(
                "response_type", "in_channel",
                "text", "*Search Results*: " + query + "\n\n" + response
        );
    }

    /**
     * Handle the help command
     *
     * @return Help text response
     */
    private Map<String, Object> handleHelpCommand() {
        return Map.of(
                "response_type", "ephemeral",
                "text", "PaperLink helps you access your organization's knowledge base.\n" +
                        "Available commands:\n" +
                        "• `/paperlink-search [query]` - Search your knowledge base\n" +
                        "• `/paperlink-help` - Show this help message"
        );
    }

    /**
     * Determine if a message should be processed
     *
     * @param channel   Channel ID
     * @param eventType Event type
     * @param text      Message text
     * @param botUserId Bot user ID
     * @return true if the message should be processed
     */
    private boolean shouldProcessMessage(String channel, String eventType, String text, String botUserId) {
        // Direct messages
        if (channel != null && channel.startsWith("D")) {
            return true;
        }

        // Mentions in channels
        if ("app_mention".equals(eventType)) {
            return true;
        }

        // Messages containing mentions of the bot
        if (text != null && botUserId != null && text.contains("<@" + botUserId + ">")) {
            return true;
        }

        return false;
    }

    /**
     * Clean message text by removing bot mentions
     *
     * @param text      Message text
     * @param botUserId Bot user ID
     * @return Cleaned text
     */
    private String cleanMessageText(String text, String botUserId) {
        if (text != null && botUserId != null && text.contains("<@" + botUserId + ">")) {
            return text.replace("<@" + botUserId + ">", "").trim();
        }
        return text;
    }

    /**
     * Extract bot user ID from payload
     *
     * @param payload Event payload
     * @return Bot user ID or empty string if not found
     */
    private String getBotUserId(Map<String, Object> payload) {
        // Try to extract bot user ID from payload
        if (payload.containsKey("authorizations")) {
            List<Map<String, Object>> authorizations = (List<Map<String, Object>>) payload.get("authorizations");
            if (!authorizations.isEmpty()) {
                return (String) authorizations.get(0).get("user_id");
            }
        }

        // Fallback
        return "";
    }
}