package com.steverhoton.poc.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for creating new items via POST endpoint.
 *
 * <p>Contains a map of attribute names to string values that will be stored in DynamoDB.
 */
public class CreateItemRequest {

  @JsonProperty("attributes")
  private Map<String, String> attributes;

  public CreateItemRequest() {}

  public CreateItemRequest(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }
}
