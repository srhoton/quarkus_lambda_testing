output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.basic_lambda.arn
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.basic_lambda.function_name
}

output "lambda_function_invoke_arn" {
  description = "Invoke ARN of the Lambda function"
  value       = aws_lambda_function.basic_lambda.invoke_arn
}

output "api_gateway_id" {
  description = "ID of the API Gateway"
  value       = aws_apigatewayv2_api.basic_lambda_api.id
}

output "api_gateway_endpoint" {
  description = "Endpoint URL of the API Gateway"
  value       = aws_apigatewayv2_api.basic_lambda_api.api_endpoint
}

output "api_gateway_invoke_url" {
  description = "Base invoke URL for the API endpoints"
  value       = "${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda"
}

output "api_endpoints" {
  description = "Available API endpoints for CRUD operations"
  value = {
    list_items   = "GET ${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda?limit=50&cursor=<cursor>"
    create_item  = "POST ${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda/{id}"
    update_item  = "PUT ${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda/{id}"
    delete_item  = "DELETE ${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda/{id}"
  }
}

output "lambda_execution_role_arn" {
  description = "ARN of the Lambda execution role"
  value       = aws_iam_role.lambda_execution_role.arn
}

output "lambda_alias_arn" {
  description = "ARN of the Lambda alias (SnapStart-enabled)"
  value       = aws_lambda_alias.lambda_live.arn
}

output "lambda_alias_invoke_arn" {
  description = "Invoke ARN of the Lambda alias (used by API Gateway)"
  value       = aws_lambda_alias.lambda_live.invoke_arn
}

output "lambda_version" {
  description = "Published version of the Lambda function"
  value       = aws_lambda_function.basic_lambda.version
}

output "lambda_snapstart_status" {
  description = "SnapStart optimization status"
  value       = "Enabled on PublishedVersions"
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table"
  value       = aws_dynamodb_table.quarkus_lambda_table.name
}

output "dynamodb_table_arn" {
  description = "ARN of the DynamoDB table"
  value       = aws_dynamodb_table.quarkus_lambda_table.arn
}

output "dynamodb_stream_arn" {
  description = "ARN of the DynamoDB stream"
  value       = aws_dynamodb_table.quarkus_lambda_table.stream_arn
}

output "dynamodb_table_id" {
  description = "ID of the DynamoDB table"
  value       = aws_dynamodb_table.quarkus_lambda_table.id
}