package com.paperlink.server.controller;


import com.paperlink.server.dtos.requests.ChatRequestDto;
import com.paperlink.server.dtos.response.ChatResponseDto;
import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.exceptions.ErrorResponse;
import com.paperlink.server.services.KnowledgeSourceService;
import com.paperlink.server.services.vector.DocumentService;
import com.paperlink.server.services.vector.VectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI API", description = "API endpoints for document processing and AI chat")
@RequestMapping("/v1")
public class AiController {

    private final VectorService vectorService;
    private final DocumentService documentService;
    private final KnowledgeSourceService knowledgeSourceService;

    @Operation(
            summary = "Get AI response to a query",
            description = "Sends a query to the AI model and retrieves a response",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful query",
                            content = @Content(schema = @Schema(implementation = ChatResponseDto.class))
                    ),
                    @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping("/chat")
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        String response = vectorService.getResponse(request.getQuery(), request.getConversationId());
        return new ChatResponseDto(response);
    }

    @Operation(
            summary = "Upload documents for processing",
            description = "Upload PDF documents for parsing and processing into the vector database",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Documents uploaded successfully"
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid file format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping(value = "/documents/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public List<KnowledgeSourceEntity> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) {
        return documentService.uploadDocumentAndParseIt(files);
    }

    @Operation(
            summary = "Get processing status",
            description = "Get the processing status of a document",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status retrieved successfully"
                    ),
                    @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/documents/{id}/status")
    public KnowledgeSourceEntity getDocumentStatus(@PathVariable String id) {
        return knowledgeSourceService.getKnowledgeSource(id);
    }
}