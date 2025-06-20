package com.steverhoton.poc.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.steverhoton.poc.model.CreateItemRequest;
import com.steverhoton.poc.model.DynamoDbItem;
import com.steverhoton.poc.model.ItemResponse;
import com.steverhoton.poc.model.ListItemsResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Service class for handling DynamoDB operations.
 *
 * <p>Implements CRUD operations with proper error handling and business logic.
 */
@ApplicationScoped
public class ItemService {

  private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

  private static final String TABLE_NAME = "quarkus-lambda-testing";
  private static final String PK_ATTRIBUTE = "PK";
  private static final String SK_ATTRIBUTE = "SK";
  private static final String STATUS_CREATED = "created";
  private static final String STATUS_IN_PROGRESS = "in_progress";
  private static final String CREATED_AT_ATTRIBUTE = "created_at";
  private static final String UPDATED_AT_ATTRIBUTE = "updated_at";

  @Inject DynamoDbClient dynamoDbClient;

  /**
   * Creates a new item in DynamoDB.
   *
   * @param id the unique identifier for the item
   * @param request the create request containing attributes
   * @return the created item response
   * @throws RuntimeException if the item already exists or database operation fails
   */
  public ItemResponse createItem(String id, CreateItemRequest request) {
    logger.debug("Creating item with id: {}", id);

    try {
      DynamoDbItem item = DynamoDbItem.fromCreateRequest(id, request);
      Map<String, AttributeValue> itemMap = buildItemAttributeMap(item);

      PutItemRequest putRequest =
          PutItemRequest.builder()
              .tableName(TABLE_NAME)
              .item(itemMap)
              .conditionExpression("attribute_not_exists(#pk)")
              .expressionAttributeNames(Map.of("#pk", PK_ATTRIBUTE))
              .build();

      dynamoDbClient.putItem(putRequest);

      logger.info("Successfully created item with id: {}", id);
      return item.toItemResponse();

    } catch (ConditionalCheckFailedException e) {
      logger.warn("Attempt to create item that already exists: {}", id);
      throw new RuntimeException("Item with id '" + id + "' already exists", e);
    } catch (DynamoDbException e) {
      logger.error("DynamoDB error creating item with id: {}", id, e);
      throw new RuntimeException("Failed to create item: " + e.getMessage(), e);
    }
  }

  /**
   * Updates an existing item from 'created' to 'in_progress' status.
   *
   * @param id the unique identifier for the item
   * @return the updated item response
   * @throws RuntimeException if the item doesn't exist, is not in 'created' status, or database
   *     operation fails
   */
  public ItemResponse updateItem(String id) {
    logger.debug("Updating item with id: {}", id);

    try {
      // First check if item exists and is in 'created' status
      Optional<DynamoDbItem> existingItem = findItem(id, STATUS_CREATED);
      if (existingItem.isEmpty()) {
        // Check if item exists with 'in_progress' status
        Optional<DynamoDbItem> inProgressItem = findItem(id, STATUS_IN_PROGRESS);
        if (inProgressItem.isPresent()) {
          throw new RuntimeException("Item with id '" + id + "' is already in progress");
        }
        throw new RuntimeException("Item with id '" + id + "' not found or not in created status");
      }

      DynamoDbItem item = existingItem.get();
      Instant now = Instant.now();

      // Since SK is part of the key, we need to delete the old item and create a new one
      // 1. Delete the old item with SK="created"
      DeleteItemRequest deleteRequest =
          DeleteItemRequest.builder()
              .tableName(TABLE_NAME)
              .key(
                  Map.of(
                      PK_ATTRIBUTE, AttributeValue.fromS(id),
                      SK_ATTRIBUTE, AttributeValue.fromS(STATUS_CREATED)))
              .conditionExpression("attribute_exists(#pk) AND #sk = :sk")
              .expressionAttributeNames(Map.of("#pk", PK_ATTRIBUTE, "#sk", SK_ATTRIBUTE))
              .expressionAttributeValues(Map.of(":sk", AttributeValue.fromS(STATUS_CREATED)))
              .build();

      dynamoDbClient.deleteItem(deleteRequest);

      // 2. Create a new item with SK="in_progress"
      Map<String, AttributeValue> newItemAttributes = new HashMap<>();
      newItemAttributes.put(PK_ATTRIBUTE, AttributeValue.fromS(id));
      newItemAttributes.put(SK_ATTRIBUTE, AttributeValue.fromS(STATUS_IN_PROGRESS));
      newItemAttributes.put(
          CREATED_AT_ATTRIBUTE, AttributeValue.fromS(item.getCreatedAt().toString()));
      newItemAttributes.put(UPDATED_AT_ATTRIBUTE, AttributeValue.fromS(now.toString()));

      // Add all the existing attributes
      if (item.getAttributes() != null) {
        for (Map.Entry<String, String> entry : item.getAttributes().entrySet()) {
          newItemAttributes.put(entry.getKey(), AttributeValue.fromS(entry.getValue()));
        }
      }

      PutItemRequest putRequest =
          PutItemRequest.builder().tableName(TABLE_NAME).item(newItemAttributes).build();

      dynamoDbClient.putItem(putRequest);

      // Update the item object for response
      item.setSk(STATUS_IN_PROGRESS);
      item.setUpdatedAt(now);

      logger.info("Successfully updated item with id: {}", id);
      return item.toItemResponse();

    } catch (ConditionalCheckFailedException e) {
      logger.warn("Conditional check failed when updating item: {}", id);
      throw new RuntimeException("Item with id '" + id + "' not found or not in created status", e);
    } catch (DynamoDbException e) {
      logger.error("DynamoDB error updating item with id: {}", id, e);
      throw new RuntimeException("Failed to update item: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes an item by its ID.
   *
   * @param id the unique identifier for the item
   * @throws RuntimeException if the item doesn't exist or database operation fails
   */
  public void deleteItem(String id) {
    logger.debug("Deleting item with id: {}", id);

    try {
      // Check if item exists (in either status)
      Optional<DynamoDbItem> createdItem = findItem(id, STATUS_CREATED);
      Optional<DynamoDbItem> inProgressItem = findItem(id, STATUS_IN_PROGRESS);

      if (createdItem.isEmpty() && inProgressItem.isEmpty()) {
        throw new RuntimeException("Item with id '" + id + "' not found");
      }

      String statusToDelete = createdItem.isPresent() ? STATUS_CREATED : STATUS_IN_PROGRESS;

      DeleteItemRequest deleteRequest =
          DeleteItemRequest.builder()
              .tableName(TABLE_NAME)
              .key(
                  Map.of(
                      PK_ATTRIBUTE, AttributeValue.fromS(id),
                      SK_ATTRIBUTE, AttributeValue.fromS(statusToDelete)))
              .conditionExpression("attribute_exists(#pk)")
              .expressionAttributeNames(Map.of("#pk", PK_ATTRIBUTE))
              .build();

      dynamoDbClient.deleteItem(deleteRequest);

      logger.info("Successfully deleted item with id: {}", id);

    } catch (ConditionalCheckFailedException e) {
      logger.warn("Conditional check failed when deleting item: {}", id);
      throw new RuntimeException("Item with id '" + id + "' not found", e);
    } catch (DynamoDbException e) {
      logger.error("DynamoDB error deleting item with id: {}", id, e);
      throw new RuntimeException("Failed to delete item: " + e.getMessage(), e);
    }
  }

  /**
   * Lists all items with cursor-based pagination.
   *
   * @param cursor the pagination cursor (base64 encoded)
   * @param limit the maximum number of items to return (default: 50)
   * @return the list response with items and pagination information
   */
  public ListItemsResponse listItems(String cursor, Integer limit) {
    int actualLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 50;
    logger.debug("Listing items with cursor: {}, limit: {}", cursor, actualLimit);

    try {
      ScanRequest.Builder scanBuilder =
          ScanRequest.builder().tableName(TABLE_NAME).limit(actualLimit + 1); // +1 to check hasMore

      // Add cursor if provided
      if (cursor != null && !cursor.trim().isEmpty()) {
        try {
          Map<String, AttributeValue> exclusiveStartKey = decodeCursor(cursor);
          scanBuilder.exclusiveStartKey(exclusiveStartKey);
        } catch (Exception e) {
          logger.warn("Invalid cursor provided: {}", cursor, e);
          throw new RuntimeException("Invalid cursor format");
        }
      }

      ScanResponse response = dynamoDbClient.scan(scanBuilder.build());
      List<Map<String, AttributeValue>> items = response.items();

      // Check if there are more items
      boolean hasMore = items.size() > actualLimit;
      if (hasMore) {
        items = items.subList(0, actualLimit); // Remove the extra item
      }

      // Convert items to response objects
      List<ItemResponse> itemResponses = new ArrayList<>();
      for (Map<String, AttributeValue> item : items) {
        try {
          DynamoDbItem dynamoItem = mapAttributesToItem(item);
          itemResponses.add(dynamoItem.toItemResponse());
        } catch (Exception e) {
          logger.warn("Failed to convert DynamoDB item to response", e);
        }
      }

      // Generate next cursor if there are more items
      String nextCursor = null;
      if (hasMore && !items.isEmpty()) {
        Map<String, AttributeValue> lastItem = items.get(items.size() - 1);
        nextCursor = encodeCursor(lastItem);
      }

      logger.info("Successfully listed {} items, hasMore: {}", itemResponses.size(), hasMore);
      return new ListItemsResponse(itemResponses, nextCursor, hasMore, itemResponses.size());

    } catch (DynamoDbException e) {
      logger.error("DynamoDB error listing items", e);
      throw new RuntimeException("Failed to list items: " + e.getMessage(), e);
    }
  }

  /** Finds an item by ID and status. */
  private Optional<DynamoDbItem> findItem(String id, String status) {
    try {
      GetItemRequest getRequest =
          GetItemRequest.builder()
              .tableName(TABLE_NAME)
              .key(
                  Map.of(
                      PK_ATTRIBUTE, AttributeValue.fromS(id),
                      SK_ATTRIBUTE, AttributeValue.fromS(status)))
              .build();

      GetItemResponse response = dynamoDbClient.getItem(getRequest);
      if (response.hasItem()) {
        return Optional.of(mapAttributesToItem(response.item()));
      }
      return Optional.empty();
    } catch (ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Builds attribute map for DynamoDB operations. */
  private Map<String, AttributeValue> buildItemAttributeMap(DynamoDbItem item) {
    Map<String, AttributeValue> attributes = new HashMap<>();
    attributes.put(PK_ATTRIBUTE, AttributeValue.fromS(item.getPk()));
    attributes.put(SK_ATTRIBUTE, AttributeValue.fromS(item.getSk()));
    attributes.put(CREATED_AT_ATTRIBUTE, AttributeValue.fromS(item.getCreatedAt().toString()));
    attributes.put(UPDATED_AT_ATTRIBUTE, AttributeValue.fromS(item.getUpdatedAt().toString()));

    // Add custom attributes
    for (Map.Entry<String, String> entry : item.getAttributes().entrySet()) {
      attributes.put(entry.getKey(), AttributeValue.fromS(entry.getValue()));
    }

    return attributes;
  }

  /** Maps DynamoDB attributes to internal item model. */
  private DynamoDbItem mapAttributesToItem(Map<String, AttributeValue> attributes) {
    DynamoDbItem item = new DynamoDbItem();
    item.setPk(attributes.get(PK_ATTRIBUTE).s());
    item.setSk(attributes.get(SK_ATTRIBUTE).s());

    if (attributes.containsKey(CREATED_AT_ATTRIBUTE)) {
      item.setCreatedAt(Instant.parse(attributes.get(CREATED_AT_ATTRIBUTE).s()));
    }
    if (attributes.containsKey(UPDATED_AT_ATTRIBUTE)) {
      item.setUpdatedAt(Instant.parse(attributes.get(UPDATED_AT_ATTRIBUTE).s()));
    }

    // Extract custom attributes (exclude system attributes)
    Map<String, String> customAttributes = new HashMap<>();
    for (Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
      String key = entry.getKey();
      if (!key.equals(PK_ATTRIBUTE)
          && !key.equals(SK_ATTRIBUTE)
          && !key.equals(CREATED_AT_ATTRIBUTE)
          && !key.equals(UPDATED_AT_ATTRIBUTE)) {
        customAttributes.put(key, entry.getValue().s());
      }
    }
    item.setAttributes(customAttributes);

    return item;
  }

  /** Encodes pagination cursor from DynamoDB item. */
  private String encodeCursor(Map<String, AttributeValue> lastItem) {
    try {
      String pk = lastItem.get(PK_ATTRIBUTE).s();
      String sk = lastItem.get(SK_ATTRIBUTE).s();
      String cursorData = pk + "|" + sk;
      return Base64.getEncoder().encodeToString(cursorData.getBytes());
    } catch (Exception e) {
      logger.warn("Failed to encode cursor", e);
      return null;
    }
  }

  /** Decodes pagination cursor to DynamoDB exclusive start key. */
  private Map<String, AttributeValue> decodeCursor(String cursor) {
    try {
      String decoded = new String(Base64.getDecoder().decode(cursor));
      String[] parts = decoded.split("\\|");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid cursor format");
      }
      return Map.of(
          PK_ATTRIBUTE, AttributeValue.fromS(parts[0]),
          SK_ATTRIBUTE, AttributeValue.fromS(parts[1]));
    } catch (Exception e) {
      throw new RuntimeException("Invalid cursor format", e);
    }
  }
}
