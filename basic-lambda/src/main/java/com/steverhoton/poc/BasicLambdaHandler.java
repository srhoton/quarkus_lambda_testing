package com.steverhoton.poc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * AWS Lambda handler that receives events and outputs them to standard error.
 *
 * <p>This basic lambda function demonstrates receiving AWS Lambda events and logging them to stderr
 * for debugging and monitoring purposes.
 *
 * @author AI Assistant
 * @version 1.0.0
 */
@RegisterForReflection
public class BasicLambdaHandler implements RequestHandler<Object, String> {

  private static final Logger logger = LoggerFactory.getLogger(BasicLambdaHandler.class);

  /**
   * Handles the Lambda function invocation.
   *
   * <p>This method receives the event object from AWS Lambda and outputs it to standard error for
   * debugging purposes. It returns a simple acknowledgment message.
   *
   * @param event the input event object from AWS Lambda
   * @param context the Lambda context object containing runtime information
   * @return a confirmation message indicating the event was processed
   * @throws RuntimeException if event processing fails
   */
  @Override
  public String handleRequest(Object event, Context context) {
    String requestId = null;
    try {
      requestId = context.getAwsRequestId();

      // Log basic context information
      logger.info(
          "Processing Lambda request - RequestId: {}, Function: {}",
          requestId,
          context.getFunctionName());

      // Output the received event to stderr as requested
      System.err.println("=== LAMBDA EVENT RECEIVED ===");
      System.err.println("RequestId: " + requestId);
      System.err.println("Function Name: " + context.getFunctionName());
      System.err.println("Remaining Time: " + context.getRemainingTimeInMillis() + "ms");
      System.err.println("Event Object: " + (event != null ? event.toString() : "null"));
      System.err.println("Event Class: " + (event != null ? event.getClass().getName() : "null"));
      System.err.println("=============================");

      logger.info("Successfully processed Lambda event");
      return "Event processed successfully - RequestId: " + requestId;

    } catch (Exception e) {
      // Handle cases where context methods might fail
      String safeRequestId = requestId != null ? requestId : "unknown";
      logger.error("Error processing Lambda event - RequestId: {}", safeRequestId, e);

      // Also output error to stderr
      System.err.println("ERROR: Failed to process Lambda event - " + e.getMessage());

      throw new RuntimeException("Failed to process Lambda event", e);
    }
  }
}
