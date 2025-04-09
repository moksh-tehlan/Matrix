package com.paperlink.server.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.paperlink.server.dtos.enums.ProcessingStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Knowledge Source Used for request/response operations related to
 * Knowledge Sources
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeSourceDto {

  // Unique identifier
  private String id;

  // Original filename
  private String name;

  // Processing status (PROCESSING, COMPLETED, FAILED)
  private ProcessingStatus processingStatus;

  // S3 path
  private String path;

  // File size in bytes
  private Long fileSize;

  // MIME type of the file
  private String mimeType;

  // Creation timestamp
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  // Last update timestamp
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;

  // Processing completion percentage (0-100)
  private Integer processingProgress;

  // Number of chunks extracted from the document
  private Integer chunkCount;

  // Error message if processing failed
  private String errorMessage;

  // User ID who uploaded the document
  private String userId;

  /** Request DTO for creating a new Knowledge Source */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    private String name;
    private String userId;
  }

  /** Request DTO for updating a Knowledge Source */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;
    private ProcessingStatus processingStatus;
    private Integer processingProgress;
    private String errorMessage;
  }
}
