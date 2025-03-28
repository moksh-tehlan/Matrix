package com.paperlink.server.controller;


import com.paperlink.server.dtos.requests.ChatRequestDto;
import com.paperlink.server.dtos.response.ChatResponseDto;
import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.services.DocumentService;
import com.paperlink.server.services.KnowledgeSourceService;
import com.paperlink.server.services.vector.VectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI API", description = "API endpoints for document processing and AI chat")
@RequestMapping("/api/v1")
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
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {
        String response = vectorService.getResponse(request.getQuery(), request.getConversationId());
        return ResponseEntity.ok(new ChatResponseDto(response));
    }

    @Operation(
            summary = "Upload documents for processing",
            description = "Upload PDF documents for parsing and processing into the vector database",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Documents uploaded successfully"
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid file format"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<KnowledgeSourceEntity>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) {
        List<KnowledgeSourceEntity> response = documentService.uploadDocumentAndParseIt(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get processing status",
            description = "Get the processing status of a document",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status retrieved successfully"
                    ),
                    @ApiResponse(responseCode = "404", description = "Document not found")
            }
    )
    @GetMapping("/documents/{id}/status")
    public ResponseEntity<KnowledgeSourceEntity> getDocumentStatus(@PathVariable String id) {
        KnowledgeSourceEntity knowledgeSource = knowledgeSourceService.getKnowledgeSource(id);
        return ResponseEntity.ok(knowledgeSource);
    }
}