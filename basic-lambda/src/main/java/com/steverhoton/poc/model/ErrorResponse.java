package com.steverhoton.poc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for error conditions.
 *
 * <p>Provides structured error information for API responses.
 */
public class ErrorResponse {

  @JsonProperty("error")
  private String error;

  @JsonProperty("message")
  private String message;

  @JsonProperty("code")
  private String code;

  public ErrorResponse() {}

  public ErrorResponse(String error, String message) {
    this.error = error;
    this.message = message;
  }

  public ErrorResponse(String error, String message, String code) {
    this.error = error;
    this.message = message;
    this.code = code;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
