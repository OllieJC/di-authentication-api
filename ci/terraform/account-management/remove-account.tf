module "delete_account" {
  source = "../modules/account-management-endpoint-module"

  endpoint_name   = "delete-account"
  path_part       = "delete-account"
  endpoint_method = "POST"
  handler_environment_variables = {
    ENVIRONMENT     = var.environment
    DYNAMO_ENDPOINT = var.use_localstack ? var.lambda_dynamo_endpoint : null
    EMAIL_QUEUE_URL = aws_sqs_queue.email_queue.id
  }
  handler_function_name = "uk.gov.di.accountmanagement.lambda.RemoveAccountHandler::handleRequest"

  authorizer_id             = aws_api_gateway_authorizer.di_account_management_api.id
  rest_api_id               = aws_api_gateway_rest_api.di_account_management_api.id
  root_resource_id          = aws_api_gateway_rest_api.di_account_management_api.root_resource_id
  execution_arn             = aws_api_gateway_rest_api.di_account_management_api.execution_arn
  api_deployment_stage_name = var.environment
  lambda_zip_file           = var.lambda_zip_file
  security_group_id         = aws_vpc.account_management_vpc.default_security_group_id
  subnet_id                 = aws_subnet.account_management_subnets.*.id
  environment               = var.environment
  lambda_role_arn           = aws_iam_role.dynamo_sqs_lambda_iam_role.arn
  use_localstack            = var.use_localstack
  default_tags              = local.default_tags
  logging_endpoint_enabled  = var.logging_endpoint_enabled
  logging_endpoint_arn      = var.logging_endpoint_arn
}