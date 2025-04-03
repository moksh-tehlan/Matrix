package com.paperlink.server.controller;

import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.services.JwtService;
import com.paperlink.server.services.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Tag(name = "Admin API", description = "API endpoints for admin management")
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    @Value("${slack.client.id}")
    private String slackClientId;

    @Value("${slack.redirect.uri}")
    private String slackRedirectUri;

    @GetMapping("/integrations")
    public String integrationsPage(Model model, Authentication authentication) {
        // Get current user and organization
        UserEntity user = (UserEntity) authentication.getPrincipal();
        String organizationId = user.getOrganization().getId();

        // Add variables to the model
        model.addAttribute("slackClientId", slackClientId);
        model.addAttribute("slackRedirectUri", slackRedirectUri);
        model.addAttribute("organizationId", organizationId);

        return "admin/integrations";
    }

    @GetMapping("/test-slack-integration")
    public String testIntegrationPage(Model model) {
        // Add hardcoded values for testing
        model.addAttribute("slackClientId", slackClientId);
        model.addAttribute("slackRedirectUri", slackRedirectUri);
        model.addAttribute("organizationId", "c88beebd-1055-47ba-814b-56395a2e2a22");

        return "admin/integrations";
    }

}