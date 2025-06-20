package com.steverhoton.poc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.steverhoton.poc.model.CreateItemRequest;
import com.steverhoton.poc.model.ItemResponse;
import com.steverhoton.poc.model.ListItemsResponse;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Unit tests for ItemService.
 *
 * <p>Tests all CRUD operations with various scenarios including success cases, error conditions,
 * and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock private DynamoDbClient dynamoDbClient;

  @InjectMocks private ItemService itemService;

  private CreateItemRequest createRequest;
  private Map<String, AttributeValue> sampleItem;

  @BeforeEach
  void setUp() {
    createRequest = new CreateItemRequest();
    Map<String, String> attributes = new HashMap<>();
    attributes.put("name", "Test Item");
    attributes.put("description", "A test item");
    createRequest.setAttributes(attributes);

    sampleItem = new HashMap<>();
    sampleItem.put("PK", AttributeValue.fromS("test-id"));
    sampleItem.put("SK", AttributeValue.fromS("created"));
    sampleItem.put("name", AttributeValue.fromS("Test Item"));
    sampleItem.put("description", AttributeValue.fromS("A test item"));
    sampleItem.put("created_at", AttributeValue.fromS("2024-01-01T10:00:00Z"));
    sampleItem.put("updated_at", AttributeValue.fromS("2024-01-01T10:00:00Z"));
  }

  @Nested
  @DisplayName("Create Item Tests")
  class CreateItemTests {

    @Test
    @DisplayName("Should create item successfully")
    void shouldCreateItemSuccessfully() {
      // Arrange
      String id = "test-id";

      // Act
      ItemResponse response = itemService.createItem(id, createRequest);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(id);
      assertThat(response.getStatus()).isEqualTo("created");
      assertThat(response.getAttributes()).containsEntry("name", "Test Item");
      assertThat(response.getAttributes()).containsEntry("description", "A test item");
      assertThat(response.getCreatedAt()).isNotNull();
      assertThat(response.getUpdatedAt()).isNotNull();

      verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when item already exists")
    void shouldThrowExceptionWhenItemAlreadyExists() {
      // Arrange
      String id = "existing-id";
      when(dynamoDbClient.putItem(any(PutItemRequest.class)))
          .thenThrow(ConditionalCheckFailedException.builder().build());

      // Act & Assert
      assertThatThrownBy(() -> itemService.createItem(id, createRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should throw exception on DynamoDB error")
    void shouldThrowExceptionOnDynamoDbError() {
      // Arrange
      String id = "test-id";
      when(dynamoDbClient.putItem(any(PutItemRequest.class)))
          .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

      // Act & Assert
      assertThatThrownBy(() -> itemService.createItem(id, createRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to create item");
    }
  }

  @Nested
  @DisplayName("Update Item Tests")
  class UpdateItemTests {

    @Test
    @DisplayName("Should update item successfully")
    void shouldUpdateItemSuccessfully() {
      // Arrange
      String id = "test-id";
      GetItemResponse getResponse = GetItemResponse.builder().item(sampleItem).build();
      when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getResponse);

      // Act
      ItemResponse response = itemService.updateItem(id);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(id);
      assertThat(response.getStatus()).isEqualTo("in_progress");
      assertThat(response.getAttributes()).containsEntry("name", "Test Item");

      verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
      verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when item not found")
    void shouldThrowExceptionWhenItemNotFound() {
      // Arrange
      String id = "non-existent-id";
      GetItemResponse emptyResponse = GetItemResponse.builder().build();
      when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(emptyResponse);

      // Act & Assert
      assertThatThrownBy(() -> itemService.updateItem(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw exception when item already in progress")
    void shouldThrowExceptionWhenItemAlreadyInProgress() {
      // Arrange
      String id = "test-id";
      Map<String, AttributeValue> inProgressItem = new HashMap<>(sampleItem);
      inProgressItem.put("SK", AttributeValue.fromS("in_progress"));

      GetItemResponse emptyResponse = GetItemResponse.builder().build();
      GetItemResponse inProgressResponse = GetItemResponse.builder().item(inProgressItem).build();

      when(dynamoDbClient.getItem(any(GetItemRequest.class)))
          .thenReturn(emptyResponse, inProgressResponse);

      // Act & Assert
      assertThatThrownBy(() -> itemService.updateItem(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("already in progress");
    }

    @Test
    @DisplayName("Should throw exception on update failure")
    void shouldThrowExceptionOnUpdateFailure() {
      // Arrange
      String id = "test-id";
      GetItemResponse getResponse = GetItemResponse.builder().item(sampleItem).build();
      when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getResponse);
      when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
          .thenThrow(DynamoDbException.builder().message("Update failed").build());

      // Act & Assert
      assertThatThrownBy(() -> itemService.updateItem(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to update item");
    }
  }

  @Nested
  @DisplayName("Delete Item Tests")
  class DeleteItemTests {

    @Test
    @DisplayName("Should delete item successfully")
    void shouldDeleteItemSuccessfully() {
      // Arrange
      String id = "test-id";
      GetItemResponse getResponse = GetItemResponse.builder().item(sampleItem).build();
      GetItemResponse emptyResponse = GetItemResponse.builder().build();

      when(dynamoDbClient.getItem(any(GetItemRequest.class)))
          .thenReturn(getResponse, emptyResponse);

      // Act
      itemService.deleteItem(id);

      // Assert
      verify(dynamoDbClient, times(2)).getItem(any(GetItemRequest.class));
      verify(dynamoDbClient, times(1)).deleteItem(any(DeleteItemRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when item not found")
    void shouldThrowExceptionWhenItemNotFound() {
      // Arrange
      String id = "non-existent-id";
      GetItemResponse emptyResponse = GetItemResponse.builder().build();
      when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(emptyResponse);

      // Act & Assert
      assertThatThrownBy(() -> itemService.deleteItem(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw exception on delete failure")
    void shouldThrowExceptionOnDeleteFailure() {
      // Arrange
      String id = "test-id";
      GetItemResponse getResponse = GetItemResponse.builder().item(sampleItem).build();
      GetItemResponse emptyResponse = GetItemResponse.builder().build();

      when(dynamoDbClient.getItem(any(GetItemRequest.class)))
          .thenReturn(getResponse, emptyResponse);
      doThrow(DynamoDbException.builder().message("Delete failed").build())
          .when(dynamoDbClient)
          .deleteItem(any(DeleteItemRequest.class));

      // Act & Assert
      assertThatThrownBy(() -> itemService.deleteItem(id))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to delete item");
    }
  }

  @Nested
  @DisplayName("List Items Tests")
  class ListItemsTests {

    @Test
    @DisplayName("Should list items successfully with default limit")
    void shouldListItemsSuccessfullyWithDefaultLimit() {
      // Arrange
      ScanResponse scanResponse = ScanResponse.builder().items(sampleItem).build();
      when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

      // Act
      ListItemsResponse response = itemService.listItems(null, null);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getItems()).hasSize(1);
      assertThat(response.getItems().get(0).getId()).isEqualTo("test-id");
      assertThat(response.getItems().get(0).getStatus()).isEqualTo("created");
      assertThat(response.isHasMore()).isFalse();
      assertThat(response.getCount()).isEqualTo(1);

      verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    @DisplayName("Should list items with custom limit")
    void shouldListItemsWithCustomLimit() {
      // Arrange
      ScanResponse scanResponse = ScanResponse.builder().items(sampleItem).build();
      when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

      // Act
      ListItemsResponse response = itemService.listItems(null, 10);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getItems()).hasSize(1);
      assertThat(response.isHasMore()).isFalse();
    }

    @Test
    @DisplayName("Should handle pagination with cursor")
    void shouldHandlePaginationWithCursor() {
      // Arrange
      String cursor = "dGVzdC1pZHxjcmVhdGVk"; // base64 encoded "test-id|created"
      ScanResponse scanResponse = ScanResponse.builder().items(sampleItem).build();
      when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);

      // Act
      ListItemsResponse response = itemService.listItems(cursor, 50);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception with invalid cursor")
    void shouldThrowExceptionWithInvalidCursor() {
      // Arrange
      String invalidCursor = "invalid-cursor";

      // Act & Assert
      assertThatThrownBy(() -> itemService.listItems(invalidCursor, 50))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Invalid cursor format");
    }

    @Test
    @DisplayName("Should throw exception on scan failure")
    void shouldThrowExceptionOnScanFailure() {
      // Arrange
      when(dynamoDbClient.scan(any(ScanRequest.class)))
          .thenThrow(DynamoDbException.builder().message("Scan failed").build());

      // Act & Assert
      assertThatThrownBy(() -> itemService.listItems(null, 50))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to list items");
    }
  }
}
