variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "us-east-1"

  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.aws_region))
    error_message = "AWS region must be in the format 'us-east-1', 'eu-west-1', etc."
  }
}

variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = can(regex("^[a-z]+$", var.environment))
    error_message = "Environment must contain only lowercase letters."
  }
}

variable "project_name" {
  description = "Name of the project for resource tagging"
  type        = string
  default     = "quarkus-lambda-testing"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project_name))
    error_message = "Project name must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "lambda_function_name" {
  description = "Name of the Lambda function"
  type        = string
  default     = "basic-lambda-handler"

  validation {
    condition     = can(regex("^[a-zA-Z0-9-_]+$", var.lambda_function_name))
    error_message = "Lambda function name must contain only letters, numbers, hyphens, and underscores."
  }
}

variable "lambda_timeout" {
  description = "Lambda function timeout in seconds"
  type        = number
  default     = 30

  validation {
    condition     = var.lambda_timeout >= 1 && var.lambda_timeout <= 900
    error_message = "Lambda timeout must be between 1 and 900 seconds."
  }
}

variable "lambda_memory_size" {
  description = "Lambda function memory size in MB"
  type        = number
  default     = 512

  validation {
    condition     = var.lambda_memory_size >= 128 && var.lambda_memory_size <= 10240
    error_message = "Lambda memory size must be between 128 MB and 10,240 MB."
  }
}

variable "api_gateway_name" {
  description = "Name of the API Gateway"
  type        = string
  default     = "basic-lambda-api"

  validation {
    condition     = can(regex("^[a-zA-Z0-9-_]+$", var.api_gateway_name))
    error_message = "API Gateway name must contain only letters, numbers, hyphens, and underscores."
  }
}