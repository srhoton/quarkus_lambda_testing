package com.steverhoton.poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * Unit tests for BasicLambdaHandler.
 *
 * <p>Tests verify that the handler correctly processes events and outputs them to stderr as
 * required.
 */
@DisplayName("BasicLambdaHandler Tests")
class BasicLambdaHandlerTest {

  private BasicLambdaHandler handler;

  @Mock private Context mockContext;

  private final ByteArrayOutputStream stderrCapture = new ByteArrayOutputStream();
  private final PrintStream originalStderr = System.err;
  private AutoCloseable mockCloseable;

  @BeforeEach
  void setUp() {
    mockCloseable = MockitoAnnotations.openMocks(this);
    handler = new BasicLambdaHandler();

    // Capture stderr output for testing
    System.setErr(new PrintStream(stderrCapture));

    // Setup mock context with common values
    when(mockContext.getAwsRequestId()).thenReturn("test-request-id-123");
    when(mockContext.getFunctionName()).thenReturn("test-function");
    when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Restore original stderr
    System.setErr(originalStderr);
    mockCloseable.close();
  }

  @Test
  @DisplayName("Should handle simple string event successfully")
  void shouldHandleStringEventSuccessfully() {
    // Given
    String testEvent = "Simple test event";

    // When
    String result = handler.handleRequest(testEvent, mockContext);

    // Then
    assertThat(result).isEqualTo("Event processed successfully - RequestId: test-request-id-123");

    String stderrOutput = stderrCapture.toString();
    assertThat(stderrOutput)
        .contains("=== LAMBDA EVENT RECEIVED ===")
        .contains("RequestId: test-request-id-123")
        .contains("Function Name: test-function")
        .contains("Remaining Time: 30000ms")
        .contains("Event Object: Simple test event")
        .contains("Event Class: java.lang.String")
        .contains("=============================");
  }

  @Test
  @DisplayName("Should handle map event successfully")
  void shouldHandleMapEventSuccessfully() {
    // Given
    Map<String, Object> testEvent = new HashMap<>();
    testEvent.put("key1", "value1");
    testEvent.put("key2", 42);

    // When
    String result = handler.handleRequest(testEvent, mockContext);

    // Then
    assertThat(result).isEqualTo("Event processed successfully - RequestId: test-request-id-123");

    String stderrOutput = stderrCapture.toString();
    assertThat(stderrOutput)
        .contains("=== LAMBDA EVENT RECEIVED ===")
        .contains("RequestId: test-request-id-123")
        .contains("Event Class: java.util.HashMap");
  }

  @Test
  @DisplayName("Should handle null event successfully")
  void shouldHandleNullEventSuccessfully() {
    // Given
    Object testEvent = null;

    // When
    String result = handler.handleRequest(testEvent, mockContext);

    // Then
    assertThat(result).isEqualTo("Event processed successfully - RequestId: test-request-id-123");

    String stderrOutput = stderrCapture.toString();
    assertThat(stderrOutput).contains("Event Object: null").contains("Event Class: null");
  }

  @Test
  @DisplayName("Should handle context methods throwing exceptions")
  void shouldHandleContextExceptionsGracefully() {
    // Given
    String testEvent = "test event";
    when(mockContext.getAwsRequestId()).thenThrow(new RuntimeException("Context error"));

    // When & Then
    assertThatThrownBy(() -> handler.handleRequest(testEvent, mockContext))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to process Lambda event")
        .hasCauseInstanceOf(RuntimeException.class);

    String stderrOutput = stderrCapture.toString();
    assertThat(stderrOutput).contains("ERROR: Failed to process Lambda event - Context error");
  }

  @Test
  @DisplayName("Should include all required context information in stderr output")
  void shouldIncludeAllRequiredContextInformation() {
    // Given
    String testEvent = "test";

    // When
    handler.handleRequest(testEvent, mockContext);

    // Then
    String stderrOutput = stderrCapture.toString();
    assertThat(stderrOutput)
        .contains("RequestId: test-request-id-123")
        .contains("Function Name: test-function")
        .contains("Remaining Time: 30000ms")
        .contains("Event Object: test")
        .contains("Event Class: java.lang.String");
  }

  @Test
  @DisplayName("Should return request ID in success message")
  void shouldReturnRequestIdInSuccessMessage() {
    // Given
    String testEvent = "test";
    String expectedRequestId = "unique-request-id-456";
    when(mockContext.getAwsRequestId()).thenReturn(expectedRequestId);

    // When
    String result = handler.handleRequest(testEvent, mockContext);

    // Then
    assertThat(result).isEqualTo("Event processed successfully - RequestId: " + expectedRequestId);
  }
}
