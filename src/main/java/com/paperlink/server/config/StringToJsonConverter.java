package com.paperlink.server.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperlink.server.dtos.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Specialized converter to handle String responses in ApiResponseHandler
 * <p>
 * This component is needed to properly handle String responses in ResponseBodyAdvice
 * when wrapping them in ApiResponse objects
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StringToJsonConverter {

    private final ObjectMapper objectMapper;

    /**
     * Convert an ApiResponse containing a String to JSON and write to response
     *
     * @param apiResponse ApiResponse containing String data
     * @param response    ServerHttpResponse
     * @throws Exception if serialization fails
     */
    public void convertToJson(ApiResponse<String> apiResponse, ServerHttpResponse response) throws Exception {
        // Serialize ApiResponse to JSON
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        // Set content type to application/json
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Write JSON string directly to response body
        response.getBody().write(jsonResponse.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a standard API response for String content
     *
     * @param body String content
     * @return ApiResponse containing String data
     */
    public ApiResponse<String> createApiResponse(String body) {
        return new ApiResponse<>(
                true,
                "Operation completed successfully",
                body,
                HttpStatus.OK.value()
        );
    }
}