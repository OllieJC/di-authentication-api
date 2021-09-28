module "register" {
  source = "../modules/endpoint-module"

  endpoint_name   = "register"
  path_part       = "register"
  endpoint_method = "POST"

  handler_environment_variables = {
    ENVIRONMENT          = var.environment
    BASE_URL             = local.api_base_url
    DYNAMO_ENDPOINT      = var.use_localstack ? var.lambda_dynamo_endpoint : null
    EVENTS_SNS_TOPIC_ARN = aws_sns_topic.events.arn
    LOCALSTACK_ENDPOINT  = var.use_localstack ? var.localstack_endpoint : null
  }
  handler_function_name = "uk.gov.di.authentication.clientregistry.lambda.ClientRegistrationHandler::handleRequest"

  create_endpoint           = false
  rest_api_id               = aws_api_gateway_rest_api.di_authentication_api.id
  root_resource_id          = aws_api_gateway_resource.register_resource.id
  execution_arn             = aws_api_gateway_rest_api.di_authentication_api.execution_arn
  api_deployment_stage_name = var.api_deployment_stage_name
  lambda_zip_file           = var.client_registry_api_lambda_zip_file
  security_group_id         = var.authentication_security_group_id
  subnet_id                 = var.authentication_subnet_ids
  lambda_role_arn           = var.lambda_iam_role_arn
  environment               = var.environment
  logging_endpoint_enabled  = var.logging_endpoint_enabled
  logging_endpoint_arn      = var.logging_endpoint_arn
  default_tags              = local.default_tags

  keep_lambda_warm             = var.keep_lambdas_warm
  warmer_handler_function_name = "uk.gov.di.lambdawarmer.lambda.LambdaWarmerHandler::handleRequest"
  warmer_lambda_zip_file       = var.lambda_warmer_zip_file

  use_localstack = var.use_localstack

  depends_on = [
    aws_api_gateway_rest_api.di_authentication_api,
    aws_api_gateway_resource.connect_resource,
    aws_api_gateway_resource.wellknown_resource,
    aws_api_gateway_resource.register_resource,
  ]
}