package com.paperlink.server.services.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperlink.server.dtos.enums.ProcessingStatus;
import com.paperlink.server.dtos.response.LambdaResponseDto;
import com.paperlink.server.exceptions.DocumentProcessingException;
import com.paperlink.server.services.KnowledgeSourceService;
import com.paperlink.server.services.aws.LambdaService;
import com.paperlink.server.services.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParserService {

    private final S3Service s3Service;
    private final LambdaService lambdaService;
    private final VectorService vectorService;
    private final KnowledgeSourceService knowledgeSourceService;

    /**
     * Process a document asynchronously
     *
     * @param bucketName        AWS S3 bucket name
     * @param key               AWS S3 object key
     * @param knowledgeSourceId Knowledge source ID
     */
    @Async("processExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processDocumentAsync(String bucketName, String key, String knowledgeSourceId) {
        log.info("Starting document processing for document: {}", key);
        try {
            long startTime = System.currentTimeMillis();

            // Prepare payload for Lambda
            Map<String, String> payload = new HashMap<>();
            payload.put("s3_bucket", bucketName);
            payload.put("s3_key", key);
            payload.put("knowledge_source_id", knowledgeSourceId);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Invoke Lambda with detailed logging
            log.debug("Invoking Lambda function with payload: {}", jsonPayload);
            String response = lambdaService.invokeLambdaFunction("paperlink-document-parser", jsonPayload);
            long lambdaEndTime = System.currentTimeMillis();
            log.info("Lambda processing took {} ms", (lambdaEndTime - startTime));

            // Process Lambda response
            startTime = System.currentTimeMillis();
            LambdaResponseDto lambdaResponseDto = LambdaResponseDto.fromJson(response);
            long lambdaResponseTime = System.currentTimeMillis();
            log.info("Lambda response processing took {} ms", (lambdaResponseTime - startTime));

            // Download and process chunks
            startTime = System.currentTimeMillis();
            byte[] fileContent = s3Service.downloadFile(bucketName, lambdaResponseDto.getS3Key());
            List<Document> documentList = vectorService.documentListFromJson(fileContent);

            long documentListTime = System.currentTimeMillis();
            log.info("Document list processing for {} documents took {} ms",
                    documentList.size(), (documentListTime - startTime));

            // Add to vector DB
            startTime = System.currentTimeMillis();
            vectorService.addVectorData(documentList);
            knowledgeSourceService.updateKnowledgeSourceStatus(knowledgeSourceId, ProcessingStatus.COMPLETED);
            long vectorTime = System.currentTimeMillis();
            log.info("Vector data insertion took {} ms", (vectorTime - startTime));

            log.info("Document processing completed successfully for document: {}", key);
        } catch (Exception e) {
            log.error("Failed to process document: {}", e.getMessage(), e);
            knowledgeSourceService.updateKnowledgeSourceStatus(knowledgeSourceId, ProcessingStatus.FAILED);
            throw new DocumentProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }
}
