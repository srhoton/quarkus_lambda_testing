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
  description = "Full invoke URL for the GET endpoint"
  value       = "${aws_apigatewayv2_api.basic_lambda_api.api_endpoint}/${aws_apigatewayv2_stage.default.name}/lambda"
}

output "lambda_execution_role_arn" {
  description = "ARN of the Lambda execution role"
  value       = aws_iam_role.lambda_execution_role.arn
}