package com.paperlink.server.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * DTO for chat requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    @NotNull(message = "Query cannot be empty")
    private String query;

    private String conversationId;
}
