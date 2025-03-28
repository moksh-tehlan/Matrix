package com.paperlink.server.services.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Service for AWS S3 operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    /**
     * Upload a byte array to S3
     *
     * @param bucketName S3 bucket name
     * @param key S3 object key
     * @param content Content as byte array
     */
    @Retryable(
            retryFor = {S3Exception.class, SdkException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void uploadFile(String bucketName, String key, byte[] content) {
        try {
            log.debug("Uploading file to S3: bucket={}, key={}, size={} bytes",
                    bucketName, key, content.length);

            // Prepare request
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Upload to S3
            s3Client.putObject(request, RequestBody.fromBytes(content));

            log.info("Successfully uploaded file to S3: bucket={}, key={}", bucketName, key);
        } catch (S3Exception e) {
            log.error("S3 error uploading file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        } catch (SdkException e) {
            log.error("SDK error uploading file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Upload a file from a local path to S3
     *
     * @param bucketName S3 bucket name
     * @param key S3 object key
     * @param filePath Local file path
     * @throws IOException If file cannot be read
     */
    @Retryable(
            value = {S3Exception.class, SdkException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void uploadFile(String bucketName, String key, Path filePath) throws IOException {
        log.debug("Reading file from path: {}", filePath);
        byte[] fileBytes = Files.readAllBytes(filePath);
        uploadFile(bucketName, key, fileBytes);
    }

    /**
     * Download a file from S3
     *
     * @param bucketName S3 bucket name
     * @param key S3 object key
     * @return File content as byte array
     */
    @Retryable(
            value = {S3Exception.class, SdkException.class, NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public byte[] downloadFile(String bucketName, String key) {
        try {
            log.debug("Downloading file from S3: bucket={}, key={}", bucketName, key);

            // Prepare request
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Download from S3
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            byte[] bytes = objectBytes.asByteArray();

            log.info("Successfully downloaded file from S3: bucket={}, key={}, size={} bytes",
                    bucketName, key, bytes.length);

            return bytes;
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: bucket={}, key={}", bucketName, key, e);
            throw e;
        } catch (S3Exception e) {
            log.error("S3 error downloading file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        } catch (SdkException e) {
            log.error("SDK error downloading file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a file from S3
     *
     * @param bucketName S3 bucket name
     * @param key S3 object key
     */
    @Retryable(
            value = {S3Exception.class, SdkException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteFile(String bucketName, String key) {
        try {
            log.debug("Deleting file from S3: bucket={}, key={}", bucketName, key);

            // Prepare request
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Delete from S3
            s3Client.deleteObject(request);

            log.info("Successfully deleted file from S3: bucket={}, key={}", bucketName, key);
        } catch (S3Exception e) {
            log.error("S3 error deleting file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        } catch (SdkException e) {
            log.error("SDK error deleting file: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * List files in an S3 bucket with a prefix
     *
     * @param bucketName S3 bucket name
     * @param prefix Key prefix
     * @return List of object keys
     */
    public List<String> listFiles(String bucketName, String prefix) {
        try {
            log.debug("Listing files in S3: bucket={}, prefix={}", bucketName, prefix);

            // Prepare request
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            // List files
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            List<String> keys = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

            log.info("Successfully listed {} files from S3: bucket={}, prefix={}",
                    keys.size(), bucketName, prefix);

            return keys;
        } catch (S3Exception e) {
            log.error("S3 error listing files: bucket={}, prefix={}, error={}",
                    bucketName, prefix, e.getMessage(), e);
            throw e;
        } catch (SdkException e) {
            log.error("SDK error listing files: bucket={}, prefix={}, error={}",
                    bucketName, prefix, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if a file exists in S3
     *
     * @param bucketName S3 bucket name
     * @param key S3 object key
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String bucketName, String key) {
        try {
            log.debug("Checking if file exists in S3: bucket={}, key={}", bucketName, key);

            // Prepare request
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Check if file exists
            s3Client.headObject(request);

            log.debug("File exists in S3: bucket={}, key={}", bucketName, key);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("File does not exist in S3: bucket={}, key={}", bucketName, key);
            return false;
        } catch (S3Exception e) {
            log.error("S3 error checking if file exists: bucket={}, key={}, error={}",
                    bucketName, key, e.getMessage(), e);
            return false;
        }
    }
}