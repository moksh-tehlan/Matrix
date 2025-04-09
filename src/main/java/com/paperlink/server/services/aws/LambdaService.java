package com.paperlink.server.services.aws;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

/** Service for AWS Lambda operations */
@Service
@Slf4j
@RequiredArgsConstructor
public class LambdaService {

  private final LambdaClient lambdaClient;

  /**
   * Invoke a Lambda function synchronously with retry capability
   *
   * @param functionName Name of the Lambda function
   * @param payload JSON payload to send to the function
   * @return Response from the Lambda function
   */
  @Retryable(
      retryFor = {ServiceException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  public String invokeLambdaFunction(String functionName, String payload) {
    log.debug("Invoking Lambda function: {} with payload: {}", functionName, payload);

    try {
      InvokeRequest request =
          InvokeRequest.builder()
              .functionName(functionName)
              .payload(SdkBytes.fromUtf8String(payload))
              .build();

      InvokeResponse response = lambdaClient.invoke(request);

      int statusCode = response.statusCode();
      if (statusCode >= 400) {
        log.error("Lambda function {} returned error code: {}", functionName, statusCode);
        throw new RuntimeException("Lambda invocation failed with status: " + statusCode);
      }

      String result = response.payload().asUtf8String();
      log.debug("Lambda function {} returned successfully", functionName);

      return result;
    } catch (ServiceException e) {
      log.error("Error invoking Lambda function {}: {}", functionName, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Invoke a Lambda function asynchronously (fire and forget)
   *
   * @param functionName Name of the Lambda function
   * @param payload JSON payload to send to the function
   * @return Status code from the Lambda service
   */
  @Retryable(
      retryFor = {ServiceException.class},
      maxAttempts = 2,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public String invokeLambdaFunctionAsync(String functionName, String payload) {
    log.debug("Invoking Lambda function asynchronously: {}", functionName);

    try {
      InvokeRequest request =
          InvokeRequest.builder()
              .functionName(functionName)
              .invocationType(InvocationType.EVENT)
              .payload(SdkBytes.fromUtf8String(payload))
              .build();

      InvokeResponse response = lambdaClient.invoke(request);
      String statusCode = response.statusCode().toString();

      log.debug(
          "Async Lambda invocation of {} initiated with status: {}", functionName, statusCode);
      return statusCode;
    } catch (ServiceException e) {
      log.error(
          "Error invoking Lambda function asynchronously {}: {}", functionName, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * List available Lambda functions
   *
   * @return List of Lambda function names
   */
  public List<String> listFunctions() {
    log.debug("Listing Lambda functions");

    try {
      ListFunctionsResponse response = lambdaClient.listFunctions();
      return response.functions().stream()
          .map(FunctionConfiguration::functionName)
          .collect(Collectors.toList());
    } catch (ServiceException e) {
      log.error("Error listing Lambda functions: {}", e.getMessage(), e);
      throw e;
    }
  }
}
