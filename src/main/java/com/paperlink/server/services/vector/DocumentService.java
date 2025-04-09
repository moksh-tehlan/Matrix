package com.paperlink.server.services.vector;

import com.paperlink.server.dtos.enums.ProcessingStatus;
import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.exceptions.DocumentProcessingException;
import com.paperlink.server.services.KnowledgeSourceService;
import com.paperlink.server.services.aws.S3Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

  private final S3Service s3Service;
  private final ParserService parserService;
  private final KnowledgeSourceService knowledgeSourceService;

  @Value("${paperlink.s3.bucket-name:paperaitest}")
  private String bucketName;

  /**
   * Upload documents and initiate processing
   *
   * @param files List of multipart files to upload and process
   * @return List of created knowledge sources
   */
  @Transactional
  public List<KnowledgeSourceEntity> uploadDocumentAndParseIt(List<MultipartFile> files) {
    validateFiles(files);

    try {
      int numberOfThreads = Math.min(files.size(), 10);
      ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      List<KnowledgeSourceEntity> sources = new ArrayList<>();

      for (MultipartFile file : files) {
        CompletableFuture<Void> future =
            CompletableFuture.runAsync(
                () -> {
                  long startTime = System.currentTimeMillis();
                  try {
                    // Prepare file metadata
                    byte[] fileByteArray = file.getBytes();
                    String key =
                        "documents/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

                    // Create knowledge source record
                    KnowledgeSourceEntity knowledgeSource =
                        KnowledgeSourceEntity.builder()
                            .path(key)
                            .name(file.getOriginalFilename())
                            .processingStatus(ProcessingStatus.PROCESSING)
                            .fileSize(file.getSize())
                            .mimeType(file.getContentType())
                            .build();

                    // Save to database and upload to S3
                    KnowledgeSourceEntity savedKnowledgeSource =
                        knowledgeSourceService.saveKnowledgeSource(knowledgeSource);
                    s3Service.uploadFile(bucketName, key, fileByteArray);

                    long endTime = System.currentTimeMillis();
                    log.info(
                        "S3 upload for {} took {} ms",
                        file.getOriginalFilename(),
                        (endTime - startTime));

                    // Start async processing
                    parserService.processDocumentAsync(
                        bucketName, key, savedKnowledgeSource.getId());
                    sources.add(savedKnowledgeSource);
                  } catch (Exception e) {
                    log.error("Error processing file {}", file.getOriginalFilename(), e);
                    throw new DocumentProcessingException(
                        "Failed to upload/process file: " + file.getOriginalFilename(), e);
                  }
                },
                executorService);
        futures.add(future);
      }

      // Wait for all uploads to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      executorService.shutdown();

      return sources;
    } catch (Exception e) {
      log.error("Failed to upload and parse documents", e);
      throw new DocumentProcessingException(
          "Failed to upload and parse documents: " + e.getMessage(), e);
    }
  }

  /**
   * Validate the uploaded files
   *
   * @param files List of files to validate
   * @throws DocumentProcessingException if validation fails
   */
  private void validateFiles(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      throw new DocumentProcessingException("No files provided for upload");
    }

    for (MultipartFile file : files) {
      if (file.isEmpty()) {
        throw new DocumentProcessingException("One or more files are empty");
      }

      String contentType = file.getContentType();
      if (contentType == null || !contentType.equals("application/pdf")) {
        throw new DocumentProcessingException("Only PDF files are allowed");
      }
    }
  }
}
