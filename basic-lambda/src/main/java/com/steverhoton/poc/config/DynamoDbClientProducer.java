package com.steverhoton.poc.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * CDI producer for AWS DynamoDB client.
 *
 * <p>Creates and configures the DynamoDB client for dependency injection throughout the
 * application.
 */
@ApplicationScoped
public class DynamoDbClientProducer {

  /**
   * Produces a DynamoDB client instance configured for the current environment.
   *
   * @return configured DynamoDB client
   */
  @Produces
  @ApplicationScoped
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder().build();
  }
}
