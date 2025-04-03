package com.paperlink.server.controller;

import com.paperlink.server.dtos.response.ApiResponse;
import com.paperlink.server.entities.SlackUserEntity;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.repositories.SlackWorkspaceRepository;
import com.paperlink.server.services.ChatService;
import com.paperlink.server.services.SlackService;
import com.paperlink.server.services.vector.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/slack")
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final SlackService slackService;
    private final VectorService vectorService;
    private final ChatService chatService;
    private final SlackWorkspaceRepository slackWorkspaceRepository;

    /**
     * OAuth callback endpoint for Slack integration
     *
     * @param code  OAuth code from Slack
     * @param state State parameter containing organization ID
     * @return Response indicating success or failure
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<ApiResponse<String>> oauthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        try {
            // Extract organization ID from state parameter
            if (state == null || state.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Missing organization ID", HttpStatus.BAD_REQUEST.value(),
                                "State parameter is required", "/api/v1/slack/oauth/callback"));
            }

            // Complete OAuth flow
            SlackWorkspaceEntity workspace = slackService.completeOAuthFlow(code, state);

            return ResponseEntity.ok(
                    ApiResponse.success("Slack integration successful",
                            "Successfully connected workspace: " + workspace.getWorkspaceName(),
                            HttpStatus.OK.value()));

        } catch (Exception e) {
            log.error("Error during Slack OAuth: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("Failed to complete Slack integration",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            e.getMessage(),
                            "/api/v1/slack/oauth/callback"));
        }
    }

    /**
     * Endpoint for Slack events API
     *
     * @param payload Event payload from Slack
     * @return Response to Slack
     */
    @PostMapping("/events")
    public ResponseEntity<Object> handleSlackEvents(@RequestBody Map<String, Object> payload) {
        try {
            String type = (String) payload.get("type");

            // Handle URL verification challenge
            if ("url_verification".equals(type)) {
                return ResponseEntity.ok().body(Map.of("challenge", payload.get("challenge")));
            }

            // Handle other event types
            if ("event_callback".equals(type)) {
                Map<String, Object> event = (Map<String, Object>) payload.get("event");
                String eventType = (String) event.get("type");

                // Handle message events
                if ("message".equals(eventType)) {
                    handleMessageEvent(payload, event);
                }
                else if ("app_mention".equals(eventType)) {
                    handleMessageEvent(payload, event);
                }
            }

            // Always return 200 OK to acknowledge receipt
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error handling Slack event: ", e);
            // Still return 200 OK to acknowledge receipt
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Handle Slack slash commands
     * Subscribe
     *
     * @param payload Command payload from Slack
     * @return Response to Slack
     */
    @PostMapping("/commands")
    public ResponseEntity<Object> handleSlackCommands(@RequestBody Map<String, String> payload) {
        try {
            log.info("payload here is: {}", payload);
            String command = payload.get("command");
            String text = payload.get("text");
            String userId = payload.get("user_id");
            String teamId = payload.get("team_id");
            String channelId = payload.get("channel_id");

            // Handle specific commands
            switch (command) {
                case "/paperlink-search":
                    // Process search command
                    return ResponseEntity.ok().body(Map.of(
                            "response_type", "in_channel",
                            "text", "Searching for: " + text + "..."
                    ));

                case "/paperlink-help":
                    // Return help text
                    return ResponseEntity.ok().body(Map.of(
                            "response_type", "ephemeral",
                            "text", "PaperLink helps you access your organization's knowledge base.\n" +
                                    "Available commands:\n" +
                                    "• `/paperlink-search [query]` - Search your knowledge base\n" +
                                    "• `/paperlink-help` - Show this help message"
                    ));

                default:
                    return ResponseEntity.ok().body(Map.of(
                            "response_type", "ephemeral",
                            "text", "Unknown command: " + command
                    ));
            }

        } catch (Exception e) {
            log.error("Error handling Slack command: ", e);
            return ResponseEntity.ok().body(Map.of(
                    "response_type", "ephemeral",
                    "text", "An error occurred processing your command"
            ));
        }
    }

    /**
     * Process message events from Slack
     *
     * @param payload Full event payload
     * @param event   Event details
     */
    private void handleMessageEvent(Map<String, Object> payload, Map<String, Object> event) {
        log.info("Slack payload: {}", payload);
        // Extract relevant information
        String teamId = (String) payload.get("team_id");
        String userId = (String) event.get("user");
        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String eventType = (String) event.get("type");

        // Ignore bot messages to prevent loops
        if (event.containsKey("bot_id") || event.containsKey("bot_profile")) {
            return;
        }

        // Process based on event type
        boolean shouldProcess = false;

        // Direct messages
        if (channel.startsWith("D")) {
            shouldProcess = true;
        }
        // Mentions in channels (either from message.channels or app_mention events)
        else if (eventType.equals("app_mention") ||
                (text != null && text.contains("<@" + getBotUserId(payload) + ">"))) {
            shouldProcess = true;
        }

        if (shouldProcess) {
            // Clean the text (remove mentions)
            if (text != null && text.contains("<@" + getBotUserId(payload) + ">")) {
                text = text.replace("<@" + getBotUserId(payload) + ">", "").trim();
            }

            try {
                log.info("incoming text from user: {}", text);
                // Find workspace entity by Slack team ID
                Optional<SlackWorkspaceEntity> workspaceOpt = slackWorkspaceRepository.findByWorkspaceId(teamId);

                if (workspaceOpt.isPresent()) {
                    SlackWorkspaceEntity workspace = workspaceOpt.get();

                    // Process user and generate response
                    SlackUserEntity slackUser = slackService.getOrCreateSlackUser(userId, workspace.getId());

                    // Use vector service to get response
//                    String response = vectorService.getResponse(text, slackUser.getId());
                    String response = "Hi there";

                    // Send response back to Slack (to the channel, not the user)
                    slackService.sendMessage(channel, workspace.getId(), response);
                } else {
                    log.error("Workspace not found for team ID: {}", teamId);
                }
            } catch (Exception e) {
                log.error("Error processing message: ", e);
            }
        }
    }

    private String getBotUserId(Map<String, Object> payload) {
        // Try to extract bot user ID from payload
        if (payload.containsKey("authorizations")) {
            List<Map<String, Object>> authorizations = (List<Map<String, Object>>) payload.get("authorizations");
            if (!authorizations.isEmpty()) {
                return (String) authorizations.get(0).get("user_id");
            }
        }

        // Fallback to configured bot ID
        return ""; // This should be set from your SlackWorkspaceEntity
    }
}