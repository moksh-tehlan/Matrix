package com.paperlink.server.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for chat interactions.
 * Contains the user's query and an optional conversation ID for maintaining context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * The user's query or message text.
     * Cannot be null or empty.
     */
    @NotBlank(message = "Query cannot be empty")
    private String query;

    /**
     * Optional conversation ID to maintain context across multiple interactions.
     * If not provided, a new conversation will be started.
     */
    private String conversationId;
}