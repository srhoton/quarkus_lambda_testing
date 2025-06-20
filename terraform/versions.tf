terraform {
  required_version = ">= 1.0"

  backend "s3" {
    bucket = "srhoton-tfstate"
    key    = "quarkus_lambda_testing"
    region = "us-east-1"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}