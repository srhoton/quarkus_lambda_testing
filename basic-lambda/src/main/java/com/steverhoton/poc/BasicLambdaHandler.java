package com.steverhoton.poc;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steverhoton.poc.model.CreateItemRequest;
import com.steverhoton.poc.model.ErrorResponse;
import com.steverhoton.poc.model.ItemResponse;
import com.steverhoton.poc.model.ListItemsResponse;
import com.steverhoton.poc.service.ItemService;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;

/**
 * AWS Lambda handler for CRUD operations on DynamoDB items.
 *
 * <p>This Lambda function handles HTTP requests from API Gateway v2 and performs CRUD operations on
 * a DynamoDB table. It supports creating, reading, updating, and deleting items with proper error
 * handling and JSON serialization. The handler is optimized for AWS SnapStart to provide sub-second
 * cold start performance.
 *
 * @author AI Assistant
 * @version 2.0.0
 */
@RegisterForReflection
public class BasicLambdaHandler
    implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

  private static final Logger logger = LoggerFactory.getLogger(BasicLambdaHandler.class);

  @Inject ItemService itemService;

  @Inject ObjectMapper objectMapper;

  // Static initialization block for SnapStart optimization
  static {
    // Initialize logger and other static resources during snapshot creation
    logger.info("BasicLambdaHandler initialized - SnapStart ready for CRUD operations");
  }

  /**
   * Handles the Lambda function invocation from API Gateway v2.
   *
   * <p>Routes HTTP requests to appropriate CRUD operations based on the HTTP method and path.
   *
   * @param event the API Gateway v2 HTTP event
   * @param context the Lambda context object containing runtime information
   * @return HTTP response with appropriate status code and body
   */
  @Override
  public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
    String requestId = context.getAwsRequestId();
    String httpMethod = event.getRequestContext().getHttp().getMethod();
    String path = event.getRequestContext().getHttp().getPath();

    logger.info("Processing {} request to {} - RequestId: {}", httpMethod, path, requestId);

    try {
      return switch (httpMethod.toUpperCase()) {
        case "POST" -> handleCreateItem(event);
        case "PUT" -> handleUpdateItem(event);
        case "DELETE" -> handleDeleteItem(event);
        case "GET" -> handleListItems(event);
        default -> createErrorResponse(405, "Method not allowed", "UNSUPPORTED_METHOD");
      };

    } catch (Exception e) {
      logger.error("Unexpected error processing request - RequestId: {}", requestId, e);
      return createErrorResponse(500, "Internal server error", "INTERNAL_ERROR");
    }
  }

  /** Handles POST requests to create new items. */
  private APIGatewayV2HTTPResponse handleCreateItem(APIGatewayV2HTTPEvent event) {
    try {
      // Extract ID from path parameters
      String id = extractIdFromPath(event);
      if (id == null || id.trim().isEmpty()) {
        return createErrorResponse(400, "ID is required in path", "MISSING_ID");
      }

      // Parse request body
      String body = event.getBody();
      if (body == null || body.trim().isEmpty()) {
        return createErrorResponse(400, "Request body is required", "MISSING_BODY");
      }

      CreateItemRequest request = objectMapper.readValue(body, CreateItemRequest.class);
      if (request.getAttributes() == null || request.getAttributes().isEmpty()) {
        return createErrorResponse(400, "Attributes are required", "MISSING_ATTRIBUTES");
      }

      ItemResponse response = itemService.createItem(id, request);
      return createSuccessResponse(201, response);

    } catch (JsonProcessingException e) {
      logger.warn("Invalid JSON in request body", e);
      return createErrorResponse(400, "Invalid JSON format", "INVALID_JSON");
    } catch (RuntimeException e) {
      if (e.getMessage().contains("already exists")) {
        return createErrorResponse(409, e.getMessage(), "ITEM_EXISTS");
      }
      logger.error("Error creating item", e);
      return createErrorResponse(500, "Failed to create item", "CREATE_FAILED");
    }
  }

  /** Handles PUT requests to update existing items. */
  private APIGatewayV2HTTPResponse handleUpdateItem(APIGatewayV2HTTPEvent event) {
    try {
      String id = extractIdFromPath(event);
      if (id == null || id.trim().isEmpty()) {
        return createErrorResponse(400, "ID is required in path", "MISSING_ID");
      }

      ItemResponse response = itemService.updateItem(id);
      return createSuccessResponse(200, response);

    } catch (RuntimeException e) {
      if (e.getMessage().contains("not found")) {
        return createErrorResponse(404, e.getMessage(), "ITEM_NOT_FOUND");
      }
      if (e.getMessage().contains("already in progress")) {
        return createErrorResponse(400, e.getMessage(), "INVALID_STATUS_TRANSITION");
      }
      logger.error("Error updating item", e);
      return createErrorResponse(500, "Failed to update item", "UPDATE_FAILED");
    }
  }

  /** Handles DELETE requests to delete items. */
  private APIGatewayV2HTTPResponse handleDeleteItem(APIGatewayV2HTTPEvent event) {
    try {
      String id = extractIdFromPath(event);
      if (id == null || id.trim().isEmpty()) {
        return createErrorResponse(400, "ID is required in path", "MISSING_ID");
      }

      itemService.deleteItem(id);
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(204)
          .withHeaders(getDefaultHeaders())
          .build();

    } catch (RuntimeException e) {
      if (e.getMessage().contains("not found")) {
        return createErrorResponse(404, e.getMessage(), "ITEM_NOT_FOUND");
      }
      logger.error("Error deleting item", e);
      return createErrorResponse(500, "Failed to delete item", "DELETE_FAILED");
    }
  }

  /** Handles GET requests to list items with pagination. */
  private APIGatewayV2HTTPResponse handleListItems(APIGatewayV2HTTPEvent event) {
    try {
      Map<String, String> queryParams = event.getQueryStringParameters();
      String cursor = queryParams != null ? queryParams.get("cursor") : null;
      String limitStr = queryParams != null ? queryParams.get("limit") : null;

      Integer limit = null;
      if (limitStr != null && !limitStr.trim().isEmpty()) {
        try {
          limit = Integer.parseInt(limitStr);
          if (limit <= 0) {
            return createErrorResponse(400, "Limit must be a positive integer", "INVALID_LIMIT");
          }
        } catch (NumberFormatException e) {
          return createErrorResponse(400, "Invalid limit format", "INVALID_LIMIT");
        }
      }

      ListItemsResponse response = itemService.listItems(cursor, limit);
      return createSuccessResponse(200, response);

    } catch (RuntimeException e) {
      if (e.getMessage().contains("Invalid cursor")) {
        return createErrorResponse(400, e.getMessage(), "INVALID_CURSOR");
      }
      logger.error("Error listing items", e);
      return createErrorResponse(500, "Failed to list items", "LIST_FAILED");
    }
  }

  /** Extracts the ID from the path parameters. */
  private String extractIdFromPath(APIGatewayV2HTTPEvent event) {
    Map<String, String> pathParams = event.getPathParameters();
    return pathParams != null ? pathParams.get("id") : null;
  }

  /** Creates a success HTTP response with JSON body. */
  private APIGatewayV2HTTPResponse createSuccessResponse(int statusCode, Object body) {
    try {
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(statusCode)
          .withHeaders(getDefaultHeaders())
          .withBody(objectMapper.writeValueAsString(body))
          .build();
    } catch (JsonProcessingException e) {
      logger.error("Error serializing response body", e);
      return createErrorResponse(500, "Internal server error", "SERIALIZATION_ERROR");
    }
  }

  /** Creates an error HTTP response with structured error information. */
  private APIGatewayV2HTTPResponse createErrorResponse(
      int statusCode, String message, String code) {
    try {
      ErrorResponse errorResponse = new ErrorResponse("Request failed", message, code);
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(statusCode)
          .withHeaders(getDefaultHeaders())
          .withBody(objectMapper.writeValueAsString(errorResponse))
          .build();
    } catch (JsonProcessingException e) {
      logger.error("Error serializing error response", e);
      // Fallback to plain text response
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(500)
          .withHeaders(Map.of("Content-Type", "text/plain"))
          .withBody("Internal server error")
          .build();
    }
  }

  /** Returns default headers for HTTP responses. */
  private Map<String, String> getDefaultHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
    return headers;
  }
}
