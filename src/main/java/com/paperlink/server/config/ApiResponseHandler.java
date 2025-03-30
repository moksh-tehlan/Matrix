package com.paperlink.server.config;

import com.paperlink.server.dtos.response.ApiResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Global ResponseBodyAdvice to standardize API responses
 * Wraps all controller responses in a consistent ApiResponse object
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiResponseHandler implements ResponseBodyAdvice<Object> {

    private final StringToJsonConverter stringToJsonConverter;

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // First check if the return type is already an ApiResponse to avoid wrapping
        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }

        // Safely check request URI to skip Swagger/OpenAPI endpoints
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes != null) {
            String requestURI = requestAttributes.getRequest().getRequestURI();
            return !requestURI.contains("/v3/api-docs") &&
                    !requestURI.contains("/swagger-ui");
        }

        // If we can't determine the request URI, default to not wrapping the response
        // to avoid potential issues
        return false;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        // Skip processing for specific cases like error responses already handled by exception handler
        if (body instanceof ApiResponse) {
            return body;
        }

        // Handle null response (e.g., void methods)
        if (body == null) {
            return new ApiResponse<>(
                    true,
                    "Operation completed successfully",
                    null,
                    HttpStatus.OK.value()
            );
        }

        // Special handling for String responses due to Jackson limitations
        if (body instanceof String) {
            log.debug("String response received, wrapping in ApiResponse");
            try {
                ApiResponse<String> apiResponse = stringToJsonConverter.createApiResponse((String) body);
                stringToJsonConverter.convertToJson(apiResponse, response);
                return null; // Response has been written, return null to avoid further processing
            } catch (Exception e) {
                log.error("Error converting String response to JSON", e);
                // Fall back to standard behavior if conversion fails
            }
        }

        // Wrap the response body in ApiResponse
        return new ApiResponse<>(
                true,
                "Operation completed successfully",
                body,
                HttpStatus.OK.value()
        );
    }
}