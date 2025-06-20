package com.steverhoton.poc.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal model representing a DynamoDB item.
 *
 * <p>Used for mapping between DynamoDB operations and API models.
 */
public class DynamoDbItem {

  private String pk;
  private String sk;
  private Map<String, String> attributes;
  private Instant createdAt;
  private Instant updatedAt;

  public DynamoDbItem() {
    this.attributes = new HashMap<>();
  }

  public DynamoDbItem(String pk, String sk, Map<String, String> attributes) {
    this.pk = pk;
    this.sk = sk;
    this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public String getPk() {
    return pk;
  }

  public void setPk(String pk) {
    this.pk = pk;
  }

  public String getSk() {
    return sk;
  }

  public void setSk(String sk) {
    this.sk = sk;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /** Converts this DynamoDB item to an API response model. */
  public ItemResponse toItemResponse() {
    return new ItemResponse(
        this.pk,
        this.sk.equals("created") ? "created" : "in_progress",
        this.attributes,
        this.createdAt != null ? this.createdAt.toString() : null,
        this.updatedAt != null ? this.updatedAt.toString() : null);
  }

  /** Creates a DynamoDB item from a create request. */
  public static DynamoDbItem fromCreateRequest(String id, CreateItemRequest request) {
    return new DynamoDbItem(id, "created", request.getAttributes());
  }
}
