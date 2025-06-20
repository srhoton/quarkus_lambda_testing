locals {
  lambda_zip_path = "../basic-lambda/build/function.zip"
  common_tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# IAM role for Lambda execution
resource "aws_iam_role" "lambda_execution_role" {
  name = "${var.lambda_function_name}-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = local.common_tags
}

# IAM policy for Lambda basic execution
resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  role       = aws_iam_role.lambda_execution_role.name
}

# IAM policy for DynamoDB access
resource "aws_iam_role_policy" "lambda_dynamodb_policy" {
  name = "${var.lambda_function_name}-dynamodb-policy"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:BatchGetItem",
          "dynamodb:BatchWriteItem",
          "dynamodb:ConditionCheckItem",
          "dynamodb:PutItem",
          "dynamodb:DescribeTable",
          "dynamodb:DeleteItem",
          "dynamodb:GetItem",
          "dynamodb:Scan",
          "dynamodb:Query",
          "dynamodb:UpdateItem"
        ]
        Resource = [
          aws_dynamodb_table.quarkus_lambda_table.arn,
          "${aws_dynamodb_table.quarkus_lambda_table.arn}/index/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:DescribeStream",
          "dynamodb:GetRecords",
          "dynamodb:GetShardIterator",
          "dynamodb:ListStreams"
        ]
        Resource = aws_dynamodb_table.quarkus_lambda_table.stream_arn
      }
    ]
  })
}

# Lambda function with SnapStart
resource "aws_lambda_function" "basic_lambda" {
  filename         = local.lambda_zip_path
  function_name    = var.lambda_function_name
  role             = aws_iam_role.lambda_execution_role.arn
  handler          = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime          = "java21"
  timeout          = var.lambda_timeout
  memory_size      = var.lambda_memory_size
  source_code_hash = filebase64sha256(local.lambda_zip_path)
  publish          = true

  environment {
    variables = {
      QUARKUS_PROFILE = var.environment
      DYNAMODB_TABLE_NAME = aws_dynamodb_table.quarkus_lambda_table.name
    }
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic_execution
  ]

  tags = local.common_tags
}

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.lambda_function_name}"
  retention_in_days = 14

  tags = local.common_tags
}

# Lambda alias for SnapStart
resource "aws_lambda_alias" "lambda_live" {
  name             = "live"
  description      = "Live alias for SnapStart-enabled Lambda function"
  function_name    = aws_lambda_function.basic_lambda.function_name
  function_version = aws_lambda_function.basic_lambda.version
}

# API Gateway v2 (HTTP API)
resource "aws_apigatewayv2_api" "basic_lambda_api" {
  name          = var.api_gateway_name
  protocol_type = "HTTP"
  description   = "API Gateway for ${var.lambda_function_name}"

  cors_configuration {
    allow_credentials = false
    allow_headers     = ["content-type", "x-amz-date", "authorization", "x-api-key", "x-amz-security-token"]
    allow_methods     = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_origins     = ["*"]
    expose_headers    = ["date", "keep-alive"]
    max_age           = 86400
  }

  tags = local.common_tags
}

# API Gateway integration with Lambda alias
resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id           = aws_apigatewayv2_api.basic_lambda_api.id
  integration_type = "AWS_PROXY"
  integration_uri  = aws_lambda_alias.lambda_live.invoke_arn

  integration_method     = "POST"
  payload_format_version = "2.0"
}

# API Gateway routes for CRUD operations
resource "aws_apigatewayv2_route" "lambda_list_route" {
  api_id    = aws_apigatewayv2_api.basic_lambda_api.id
  route_key = "GET /lambda"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "lambda_create_route" {
  api_id    = aws_apigatewayv2_api.basic_lambda_api.id
  route_key = "POST /lambda/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "lambda_update_route" {
  api_id    = aws_apigatewayv2_api.basic_lambda_api.id
  route_key = "PUT /lambda/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

resource "aws_apigatewayv2_route" "lambda_delete_route" {
  api_id    = aws_apigatewayv2_api.basic_lambda_api.id
  route_key = "DELETE /lambda/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

# API Gateway stage
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.basic_lambda_api.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_logs.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
    })
  }

  tags = local.common_tags
}

# CloudWatch Log Group for API Gateway
resource "aws_cloudwatch_log_group" "api_gateway_logs" {
  name              = "/aws/apigateway/${var.api_gateway_name}"
  retention_in_days = 14

  tags = local.common_tags
}

# Lambda permission for API Gateway to invoke the alias
resource "aws_lambda_permission" "api_gateway_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_alias.lambda_live.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.basic_lambda_api.execution_arn}/*/*"
}

# DynamoDB table for application data
resource "aws_dynamodb_table" "quarkus_lambda_table" {
  name             = "quarkus-lambda-testing"
  billing_mode     = "PAY_PER_REQUEST"
  hash_key         = "PK"
  range_key        = "SK"
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  tags = merge(local.common_tags, {
    Name    = "quarkus-lambda-testing"
    Purpose = "Application data storage for Quarkus Lambda function"
  })
}