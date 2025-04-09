package com.paperlink.server.controller;

import com.paperlink.server.dtos.response.ApiResponse;
import com.paperlink.server.entities.SlackWorkspaceEntity;
import com.paperlink.server.services.slack.SlackAuthService;
import com.paperlink.server.services.slack.SlackEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller for Slack integration endpoints */
@RestController
@RequestMapping("/v1/slack")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Slack API", description = "API endpoints for Slack integration")
public class SlackController {

  private final SlackAuthService slackAuthService;
  private final SlackEventService slackEventService;

  /**
   * OAuth callback endpoint for Slack integration
   *
   * @param code OAuth code from Slack
   * @param state State parameter containing organization ID
   * @return Response indicating success or failure
   */
  @Operation(
      summary = "OAuth callback for Slack",
      description = "Callback endpoint for Slack OAuth integration flow")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Slack integration successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Integration failed")
      })
  @GetMapping("/oauth/callback")
  public ResponseEntity<ApiResponse<String>> oauthCallback(
      @RequestParam("code") String code, @RequestParam("state") String state) {

    // Validate state parameter
    if (state == null || state.isEmpty()) {
      throw new IllegalArgumentException("State parameter is required");
    }

    // Complete OAuth flow
    SlackWorkspaceEntity workspace = slackAuthService.completeOAuthFlow(code, state);

    return ResponseEntity.ok(
        ApiResponse.success(
            "Slack integration successful",
            "Successfully connected workspace: " + workspace.getWorkspaceName(),
            HttpStatus.OK.value()));
  }

  /**
   * Endpoint for Slack events API
   *
   * @param payload Event payload from Slack
   * @return Response to Slack
   */
  @Operation(
      summary = "Handle Slack events",
      description = "Endpoint for receiving events from Slack Events API")
  @PostMapping("/events")
  public ResponseEntity<Object> handleSlackEvents(@RequestBody Map<String, Object> payload) {
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
      if ("message".equals(eventType) || "app_mention".equals(eventType)) {
        // Process asynchronously to not block the response
        // Slack requires a quick 200 OK response
        new Thread(
                () -> {
                  try {
                    slackEventService.processMessageEvent(payload, event);
                  } catch (Exception e) {
                    log.error("Error processing Slack event asynchronously", e);
                  }
                })
            .start();
      }
    }

    // Always return 200 OK to acknowledge receipt
    return ResponseEntity.ok().build();
  }

  /**
   * Handle Slack slash commands
   *
   * @param payload Command payload from Slack
   * @return Response to Slack
   */
  @Operation(
      summary = "Handle Slack slash commands",
      description = "Endpoint for handling Slack slash commands")
  @PostMapping("/commands")
  public ResponseEntity<Object> handleSlackCommands(@RequestBody Map<String, String> payload) {
    log.debug("Received Slack command payload: {}", payload);

    // Process the command through the event service
    Map<String, Object> response = slackEventService.processSlashCommand(payload);

    return ResponseEntity.ok(response);
  }
}
