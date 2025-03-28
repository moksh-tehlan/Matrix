package com.paperlink.server.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LambdaResponseDto {

    private String statusCode;
    @JsonProperty("dest_key")
    private String s3Key;
    private String status;

    public static LambdaResponseDto fromJson(String content) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(content, LambdaResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
