resource "aws_iam_role" "lambda_iam_role" {
  name = "${var.environment}-${var.name}-sqs-lambda-role"

  assume_role_policy = var.lambda_iam_policy

  tags = {
    environment = var.environment
  }
}

resource "aws_lambda_function" "sqs_lambda" {
  filename      = var.lambda_zip_file
  function_name = "${var.environment}-${var.name}-sqs-lambda"
  role          = aws_iam_role.lambda_iam_role.arn
  handler       = var.handler_function_name
  timeout       = 30
  memory_size   = 512

  source_code_hash = filebase64sha256(var.lambda_zip_file)
  vpc_config {
    security_group_ids = [var.security_group_id]
    subnet_ids = var.subnet_id
  }
  environment {
    variables = var.handler_environment_variables
  }

  runtime = var.handler_runtime

  tags = {
    environment = var.environment
  }

  depends_on = [
    aws_iam_role.lambda_iam_role,
  ]
}

resource "aws_iam_policy" "lambda_logging_policy" {
  name        = "${var.environment}-${var.name}-sqs-lambda-logging"
  path        = "/"
  description = "IAM policy for logging from a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:CreateLogGroup"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_iam_role.name
  policy_arn = aws_iam_policy.lambda_logging_policy.arn

  depends_on = [
    aws_iam_role.lambda_iam_role,
    aws_iam_policy.lambda_logging_policy,
  ]
}

resource "aws_iam_policy" "lambda_networking_policy" {
  name        = "${var.environment}-${var.name}-sqs-lambda-networking"
  path        = "/"
  description = "IAM policy for managing VPC connection for a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeNetworkInterfaces",
        "ec2:CreateNetworkInterface",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeInstances",
        "ec2:AttachNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_networking" {
  role       = aws_iam_role.lambda_iam_role.name
  policy_arn = aws_iam_policy.lambda_networking_policy.arn

  depends_on = [
    aws_iam_role.lambda_iam_role,
    aws_iam_policy.lambda_networking_policy,
  ]
}