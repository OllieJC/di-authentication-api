resource "aws_vpc" "authentication" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    environment = var.environment
  }
}

data "aws_availability_zones" "available" {}

resource "aws_subnet" "authentication" {
  count             = length(data.aws_availability_zones.available.names)
  vpc_id            = aws_vpc.authentication.id
  cidr_block        = "10.0.${count.index}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  depends_on = [
    aws_vpc.authentication,
  ]

  tags = {
    environment = var.environment
    Name        = "${var.environment}-private-subnet-for-${data.aws_availability_zones.available.names[count.index]}"
  }
}

data "aws_vpc_endpoint_service" "sqs" {
  service = "sqs"
}

resource "aws_vpc_endpoint" "sqs" {
  vpc_endpoint_type = "Interface"
  vpc_id            = aws_vpc.authentication.id
  service_name      = data.aws_vpc_endpoint_service.sqs.service_name

  subnet_ids = aws_subnet.authentication.*.id

  security_group_ids = [
    aws_vpc.authentication.default_security_group_id
  ]

  private_dns_enabled = true

  depends_on = [
    aws_vpc.authentication,
    aws_subnet.authentication,
  ]

  tags = {
    environment = var.environment
  }
}

resource "aws_subnet" "authentication_public" {
  count             = length(data.aws_availability_zones.available.names)
  vpc_id            = aws_vpc.authentication.id
  cidr_block        = "10.0.${count.index + 128}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    environment = var.environment
    Name        = "${var.environment}-public-subnet-for-${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.authentication.id

  tags = {
    environment = var.environment
    Name        = "${var.environment}-internet-gateway-for-${aws_vpc.authentication.id}"
  }
}

resource "aws_eip" "nat_gateway_eip" {
  count = length(data.aws_availability_zones.available.names)
  vpc   = true

  tags = {
    environment = var.environment
    Name        = "${var.environment}-nat-gateway-ip-for-${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_nat_gateway" "nat_gateway" {
  count = length(data.aws_availability_zones.available.names)

  allocation_id = aws_eip.nat_gateway_eip[count.index].id
  subnet_id     = aws_subnet.authentication_public[count.index].id

  tags = {
    environment = var.environment
    Name        = "${var.environment}-nat-gateway-for-${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.authentication.id

  tags = {
    environment = var.environment
    Name        = "${var.environment}-public-route-table-for-${aws_vpc.authentication.id}"
  }
}

resource "aws_route" "public_to_internet" {
  route_table_id         = aws_route_table.public_route_table.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.igw.id
}

resource "aws_route_table_association" "public_to_internet" {
  count = length(data.aws_availability_zones.available.names)

  route_table_id = aws_route_table.public_route_table.id
  subnet_id      = aws_subnet.authentication_public[count.index].id
}

resource "aws_route_table" "private_route_table" {
  count = length(data.aws_availability_zones.available.names)

  vpc_id = aws_vpc.authentication.id

  tags = {
    environment = var.environment
    Name        = "${var.environment}-private-route-table-for-${data.aws_availability_zones.available.names[count.index]}"
  }
}

resource "aws_route_table_association" "private" {
  count = length(data.aws_availability_zones.available.names)

  route_table_id = aws_route_table.private_route_table[count.index].id
  subnet_id      = aws_subnet.authentication[count.index].id
}

resource "aws_route" "private_to_internet" {
  count = length(data.aws_availability_zones.available.names)

  route_table_id         = aws_route_table.private_route_table[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.nat_gateway[count.index].id

  depends_on = [
    aws_route_table.private_route_table,
    aws_route_table_association.private,
  ]
}
