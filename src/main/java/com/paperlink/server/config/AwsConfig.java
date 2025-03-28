package com.paperlink.server.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key:#{null}}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:#{null}}")
    private String secretKey;

    /**
     * Configure AWS credentials provider
     *
     * @return AwsCredentialsProvider
     */
    @Bean
    public AwsCredentialsProvider credentialsProvider() {
        if (accessKey != null && secretKey != null) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        }
        return DefaultCredentialsProvider.create();
    }

    /**
     * Configure S3 client
     *
     * @param credentialsProvider AWS credentials provider
     * @return S3Client
     */
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Configure Lambda client
     *
     * @param credentialsProvider AWS credentials provider
     * @return LambdaClient
     */
    @Bean
    public LambdaClient lambdaClient(AwsCredentialsProvider credentialsProvider) {
        return LambdaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}