package com.steverhoton.poc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Basic tests for BasicLambdaHandler.
 *
 * <p>Simple tests to verify the handler class compiles and basic functionality works.
 */
@DisplayName("BasicLambdaHandler Tests")
class BasicLambdaHandlerTest {

  @Test
  @DisplayName("Should instantiate handler successfully")
  void shouldInstantiateHandlerSuccessfully() {
    // Given & When
    BasicLambdaHandler handler = new BasicLambdaHandler();

    // Then
    assertThat(handler).isNotNull();
  }
}
