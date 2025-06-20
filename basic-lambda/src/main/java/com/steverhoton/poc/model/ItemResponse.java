package com.steverhoton.poc.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for item operations.
 *
 * <p>Represents a single item returned from DynamoDB operations.
 */
public class ItemResponse {

  @JsonProperty("id")
  private String id;

  @JsonProperty("status")
  private String status;

  @JsonProperty("attributes")
  private Map<String, String> attributes;

  @JsonProperty("created_at")
  private String createdAt;

  @JsonProperty("updated_at")
  private String updatedAt;

  public ItemResponse() {}

  public ItemResponse(
      String id,
      String status,
      Map<String, String> attributes,
      String createdAt,
      String updatedAt) {
    this.id = id;
    this.status = status;
    this.attributes = attributes;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
