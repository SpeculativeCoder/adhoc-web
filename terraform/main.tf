#
# Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

terraform {
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "2.4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.5.1"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

variable "adhoc_domain" {
  type        = string
  nullable    = false
  description = "Domain in which Adhoc operates e.g. example.com"
  #default     = "example.com"
}

provider "local" {
}

provider "random" {
}

provider "aws" {
  # TODO
  region = (terraform.workspace == "prod" || terraform.workspace == "qa") ? "us-east-1" : "eu-west-1"
  profile = "adhoc_admin"
}

data "aws_region" "region" {
}

data "aws_route53_zone" "adhoc" {
  name = var.adhoc_domain
}

data "aws_iam_role" "ecs_task_execution_role" {
  name = "ecsTaskExecutionRole"
}

data "local_sensitive_file" "adhoc_ca_certificate" {
  #filename = fileexists("${path.root}/../certs/adhoc-ca.cer") ? "${path.root}/../certs/adhoc-ca.cer" : "${path.root}/empty_file"
  filename = "${path.root}/../certs/adhoc-ca.cer"
}

data "local_sensitive_file" "adhoc_server_certificate" {
  #filename = fileexists("${path.root}/../certs/adhoc.cer") ? "${path.root}/../certs/adhoc.cer" : "${path.root}/empty_file"
  filename = "${path.root}/../certs/adhoc.cer"
}

data "local_sensitive_file" "adhoc_private_key" {
  #filename = fileexists("${path.root}/../certs/adhoc.key") ? "${path.root}/../certs/adhoc.key" : "${path.root}/empty_file"
  filename = "${path.root}/../certs/adhoc.key"
}

// TODO: minimal permissions
resource "aws_iam_policy" "adhoc_manager" {
  name   = "adhoc_${terraform.workspace}_manager"
  policy = jsonencode(
    {
      Version   = "2012-10-17",
      Statement = [
        {
          Sid    = "0",
          Effect = "Allow",
          Action = [
            "route53:ChangeResourceRecordSets",
            "route53:ListResourceRecordSets"
          ],
          Resource = [
            data.aws_route53_zone.adhoc.arn
          ]
        },
        {
          Sid    = "1",
          Effect = "Allow",
          Action = [
            "iam:PassRole",
          ],
          Resource = [
            data.aws_iam_role.ecs_task_execution_role.arn
          ]
        },
        {
          Sid    = "2",
          Effect = "Allow",
          Action = [
            "ecs:Poll",
            "ec2:DescribeNetworkInterfaces",
            "ec2:DescribeSubnets",
            "ec2:DescribeSecurityGroups",
            "ecs:RunTask",
            "ecs:ListTasks",
            "route53:ListHostedZones",
            "route53:ListHostedZonesByName",
            "route53:TestDNSAnswer",
            "ecs:StartTask",
            "ecs:StopTask",
            "ecs:DescribeTasks"
          ],
          Resource = "*"
        }
      ]
    }
  )
}

resource "aws_iam_group" "adhoc_manager" {
  name = "adhoc_${terraform.workspace}_manager"
}

resource "aws_iam_group_policy_attachment" "adhoc_manager" {
  group      = aws_iam_group.adhoc_manager.name
  policy_arn = aws_iam_policy.adhoc_manager.arn
}

resource "aws_iam_user" "adhoc_manager" {
  name = "adhoc_${terraform.workspace}_manager"
}

resource "aws_iam_user_group_membership" "adhoc_manager" {
  user   = aws_iam_user.adhoc_manager.name
  groups = [aws_iam_group.adhoc_manager.name]
}

resource "aws_iam_access_key" "adhoc_manager" {
  user = aws_iam_user.adhoc_manager.name
}

resource "aws_vpc" "adhoc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = {
    Name = "adhoc_${terraform.workspace}"
  }
}

resource "aws_subnet" "adhoc_a" {
  vpc_id                  = aws_vpc.adhoc.id
  cidr_block              = "10.0.0.0/24"
  # TODO: multi subnets / zones
  availability_zone       = "${data.aws_region.region.name}a"
  map_public_ip_on_launch = true
  tags                    = {
    Name = "adhoc_${terraform.workspace}_a"
  }
}

resource "aws_internet_gateway" "adhoc" {
  vpc_id = aws_vpc.adhoc.id
}

resource "aws_route" "adhoc" {
  route_table_id         = aws_vpc.adhoc.default_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.adhoc.id
}

#resource "aws_route_table_association" "adhoc" {
#  subnet_id      = aws_subnet.adhoc_a.id
#  route_table_id = aws_vpc.adhoc.default_route_table_id
#}

resource "aws_security_group" "adhoc_manager" {
  name   = "adhoc_${terraform.workspace}_manager"
  vpc_id = aws_vpc.adhoc.id
}

resource "aws_security_group" "adhoc_kiosk" {
  name   = "adhoc_${terraform.workspace}_kiosk"
  vpc_id = aws_vpc.adhoc.id
}

resource "aws_security_group" "adhoc_server" {
  name   = "adhoc_${terraform.workspace}_server"
  vpc_id = aws_vpc.adhoc.id
}

// TODO: restrict
resource "aws_security_group_rule" "adhoc_manager_ingress_https" {
  security_group_id = aws_security_group.adhoc_manager.id
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_manager_ingress_http" {
  security_group_id = aws_security_group.adhoc_manager.id
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_manager_ingress_from_kiosk" {
  security_group_id        = aws_security_group.adhoc_manager.id
  type                     = "ingress"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
  source_security_group_id = aws_security_group.adhoc_kiosk.id
}

resource "aws_security_group_rule" "adhoc_manager_ingress_from_server" {
  security_group_id        = aws_security_group.adhoc_manager.id
  type                     = "ingress"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
  source_security_group_id = aws_security_group.adhoc_server.id
}

resource "aws_security_group_rule" "adhoc_manager_egress" {
  security_group_id = aws_security_group.adhoc_manager.id
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_kiosk_ingress_https" {
  security_group_id = aws_security_group.adhoc_kiosk.id
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_kiosk_ingress_http" {
  security_group_id = aws_security_group.adhoc_kiosk.id
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_kiosk_ingress_from_manager" {
  security_group_id        = aws_security_group.adhoc_kiosk.id
  type                     = "ingress"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
  source_security_group_id = aws_security_group.adhoc_manager.id
}

resource "aws_security_group_rule" "adhoc_kiosk_egress" {
  security_group_id = aws_security_group.adhoc_kiosk.id
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_server_ingress_wss" {
  security_group_id = aws_security_group.adhoc_server.id
  type              = "ingress"
  from_port         = 8889
  to_port           = 8889
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "adhoc_server_egress" {
  security_group_id = aws_security_group.adhoc_server.id
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

// TODO
#resource "aws_security_group_rule" "adhoc_server_egress_to_manager" {
#  security_group_id        = aws_security_group.adhoc_server.id
#  type                     = "egress"
#  from_port                = 0
#  to_port                  = 0
#  protocol                 = "tcp"
#  source_security_group_id = aws_security_group.adhoc_manager.id
#}

resource "aws_ecr_repository" "adhoc_manager" {
  name         = "adhoc_${terraform.workspace}_manager"
  force_delete = true
}

resource "aws_ecr_repository" "adhoc_kiosk" {
  name         = "adhoc_${terraform.workspace}_kiosk"
  force_delete = true
}

resource "aws_ecr_repository" "adhoc_server" {
  name         = "adhoc_${terraform.workspace}_server"
  force_delete = true
}

resource "aws_ecr_lifecycle_policy" "adhoc_manager" {
  repository = aws_ecr_repository.adhoc_manager.name
  policy     = jsonencode(
    {
      rules = [
        {
          rulePriority = 1
          description  = "delete untagged"
          selection    = {
            tagStatus   = "untagged"
            countType   = "sinceImagePushed"
            countUnit   = "days"
            countNumber = 1
          },
          action = {
            type = "expire"
          }
        }
      ]
    }
  )
}

resource "aws_ecr_lifecycle_policy" "adhoc_kiosk" {
  repository = aws_ecr_repository.adhoc_kiosk.name
  policy     = jsonencode(
    {
      rules = [
        {
          rulePriority = 1
          description  = "delete untagged"
          selection    = {
            tagStatus   = "untagged"
            countType   = "sinceImagePushed"
            countUnit   = "days"
            countNumber = 1
          },
          action = {
            type = "expire"
          }
        }
      ]
    }
  )
}

resource "aws_ecr_lifecycle_policy" "adhoc_server" {
  repository = aws_ecr_repository.adhoc_server.name
  policy     = jsonencode(
    {
      rules = [
        {
          rulePriority = 1
          description  = "delete untagged"
          selection    = {
            tagStatus   = "untagged"
            countType   = "sinceImagePushed"
            countUnit   = "days"
            countNumber = 1
          },
          action = {
            type = "expire"
          }
        }
      ]
    }
  )
}

// set some initial passwords
// TODO: setting / management / rotation

resource "random_password" "adhoc_postgres_password" {
  length = 50
}

resource "random_password" "adhoc_artemis_embedded_cluster_password" {
  length = 50
}

resource "random_password" "adhoc_server_basic_auth_password" {
  length = 50
}

resource "random_password" "adhoc_default_admin_password" {
  length = 30
}

resource "random_password" "adhoc_default_user_password" {
  length  = 20
  special = false
}

resource "random_password" "adhoc_hsqldb_password" {
  length = 50
}

resource "aws_ssm_parameter" "adhoc_ca_certificate" {
  name  = "adhoc_${terraform.workspace}_ca_certificate"
  type  = "SecureString"
  tier  = "Standard"
  value = data.local_sensitive_file.adhoc_ca_certificate.content
}

resource "aws_ssm_parameter" "adhoc_server_certificate" {
  name  = "adhoc_${terraform.workspace}_server_certificate"
  type  = "SecureString"
  tier  = "Advanced"
  value = data.local_sensitive_file.adhoc_server_certificate.content
}

resource "aws_ssm_parameter" "adhoc_private_key" {
  name  = "adhoc_${terraform.workspace}_private_key"
  type  = "SecureString"
  tier  = "Standard"
  value = data.local_sensitive_file.adhoc_private_key.content
}

resource "aws_ssm_parameter" "adhoc_postgres_password" {
  name  = "adhoc_${terraform.workspace}_postgres_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_postgres_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_artemis_embedded_cluster_password" {
  name  = "adhoc_${terraform.workspace}_artemis_embedded_cluster_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_artemis_embedded_cluster_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_aws_access_key_id" {
  name  = "adhoc_${terraform.workspace}_aws_access_key_id"
  type  = "SecureString"
  tier  = "Standard"
  value = aws_iam_access_key.adhoc_manager.id
}

resource "aws_ssm_parameter" "adhoc_aws_secret_access_key" {
  name  = "adhoc_${terraform.workspace}_aws_secret_access_key"
  type  = "SecureString"
  tier  = "Standard"
  value = aws_iam_access_key.adhoc_manager.secret
}

resource "aws_ssm_parameter" "adhoc_server_basic_auth_password" {
  name  = "adhoc_${terraform.workspace}_server_basic_auth_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_server_basic_auth_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_default_admin_password" {
  name  = "adhoc_${terraform.workspace}_default_admin_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_default_admin_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_default_user_password" {
  name  = "adhoc_${terraform.workspace}_default_user_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_default_user_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_hsqldb_password" {
  name  = "adhoc_${terraform.workspace}_hsqldb_password"
  type  = "SecureString"
  tier  = "Standard"
  value = random_password.adhoc_hsqldb_password.result
  // TODO
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "adhoc_postgres_host" {
  name  = "adhoc_${terraform.workspace}_postgres_host"
  type  = "String"
  tier  = "Standard"
  // TODO
  value = "localhost"
  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_service_discovery_private_dns_namespace" "adhoc" {
  name = "adhoc-${terraform.workspace}"
  vpc  = aws_vpc.adhoc.id
}

resource "aws_service_discovery_service" "adhoc_manager" {
  name = "adhoc-${terraform.workspace}-manager"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.adhoc.id
    dns_records {
      ttl  = 60
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_service_discovery_service" "adhoc_kiosk" {
  name = "adhoc-${terraform.workspace}-kiosk"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.adhoc.id
    dns_records {
      ttl  = 60
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_cloudwatch_log_group" "adhoc_manager" {
  name              = "/ecs/adhoc_${terraform.workspace}_manager"
  // manually adjust according to needs
  retention_in_days = terraform.workspace == "prod" ? 3 : 1
  lifecycle {
    ignore_changes = [retention_in_days]
  }
}

resource "aws_cloudwatch_log_group" "adhoc_kiosk" {
  name              = "/ecs/adhoc_${terraform.workspace}_kiosk"
  // manually adjust according to needs
  retention_in_days = terraform.workspace == "prod" ? 3 : 1
  lifecycle {
    ignore_changes = [retention_in_days]
  }
}

resource "aws_cloudwatch_log_group" "adhoc_server" {
  name              = "/ecs/adhoc_${terraform.workspace}_server"
  // manually adjust according to needs
  retention_in_days = terraform.workspace == "prod" ? 3 : 1
  lifecycle {
    ignore_changes = [retention_in_days]
  }
}

resource "aws_ecs_task_definition" "adhoc_manager" {
  family                = "adhoc_${terraform.workspace}_manager"
  container_definitions = jsonencode([
    {
      name         = "adhoc_${terraform.workspace}_manager"
      image        = aws_ecr_repository.adhoc_manager.repository_url
      cpu          = 0
      links        = []
      portMappings = [
        {
          name          = "adhoc_${terraform.workspace}_manager-443-tcp"
          containerPort = 443
          hostPort      = 443
          protocol      = "tcp"
        },
        {
          name          = "adhoc_${terraform.workspace}_manager-80-tcp"
          containerPort = 80
          hostPort      = 80
          protocol      = "tcp"
        }
      ],
      essential  = true
      entryPoint = []
      command    = [
      ],
      environment = [
      ],
      environmentFiles = [
      ],
      mountPoints = [
      ],
      volumesFrom = [
      ],
      secrets = [
        {
          name      = "CA_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_ca_certificate"
        },
        {
          name      = "SERVER_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_server_certificate"
        },
        {
          name      = "PRIVATE_KEY"
          valueFrom = "adhoc_${terraform.workspace}_private_key"
        },
        {
          name      = "POSTGRES_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_postgres_password"
        },
        {
          name      = "ARTEMIS_EMBEDDED_CLUSTER_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_artemis_embedded_cluster_password"
        },
        {
          name      = "AWS_ACCESS_KEY_ID"
          valueFrom = "adhoc_${terraform.workspace}_aws_access_key_id"
        },
        {
          name      = "AWS_SECRET_ACCESS_KEY"
          valueFrom = "adhoc_${terraform.workspace}_aws_secret_access_key"
        },
        {
          name      = "SERVER_BASIC_AUTH_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_server_basic_auth_password"
        },
        {
          name      = "DEFAULT_ADMIN_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_default_admin_password"
        },
        {
          name      = "DEFAULT_USER_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_default_user_password"
        },
        {
          name      = "HSQLDB_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_hsqldb_password"
        },
        {
          name      = "POSTGRES_HOST"
          valueFrom = "adhoc_${terraform.workspace}_postgres_host"
        }
      ],
      dnsServers       = []
      dnsSearchDomains = []
      extraHosts       = [
      ],
      dockerSecurityOptions = []
      dockerLabels          = {}
      ulimits               = []
      logConfiguration      = {
        logDriver = "awslogs",
        options   = {
          awslogs-create-group  = "true"
          awslogs-group         = "/ecs/adhoc_${terraform.workspace}_manager"
          awslogs-region        = data.aws_region.region.name
          awslogs-stream-prefix = "ecs"
        },
        secretOptions = [
        ]
      },
      systemControls = [
      ]
    }
  ])
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  // TODO
  #task_role_arn            = data.aws_iam_role.ecs_task_execution_role.arn
  execution_role_arn       = data.aws_iam_role.ecs_task_execution_role.arn
  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }
}

resource "aws_ecs_task_definition" "adhoc_kiosk" {
  family                = "adhoc_${terraform.workspace}_kiosk"
  container_definitions = jsonencode([
    {
      name         = "adhoc_${terraform.workspace}_kiosk"
      image        = aws_ecr_repository.adhoc_kiosk.repository_url
      cpu          = 0
      links        = []
      portMappings = [
        {
          name          = "adhoc_${terraform.workspace}_kiosk-443-tcp"
          containerPort = 443
          hostPort      = 443
          protocol      = "tcp"
        },
        {
          name          = "adhoc_${terraform.workspace}_kiosk-80-tcp"
          containerPort = 80
          hostPort      = 80
          protocol      = "tcp"
        }
      ],
      essential  = true
      entryPoint = []
      command    = [
      ],
      environment = [
      ],
      environmentFiles = [
      ],
      mountPoints = [
      ],
      volumesFrom = [
      ],
      secrets = [
        {
          name      = "CA_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_ca_certificate"
        },
        {
          name      = "SERVER_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_server_certificate"
        },
        {
          name      = "PRIVATE_KEY"
          valueFrom = "adhoc_${terraform.workspace}_private_key"
        },
        {
          name      = "POSTGRES_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_postgres_password"
        },
        {
          name      = "ARTEMIS_EMBEDDED_CLUSTER_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_artemis_embedded_cluster_password"
        },
        {
          name      = "HSQLDB_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_hsqldb_password"
        },
        {
          name      = "POSTGRES_HOST"
          valueFrom = "adhoc_${terraform.workspace}_postgres_host"
        }
      ],
      dnsServers       = []
      dnsSearchDomains = []
      extraHosts       = [
      ],
      dockerSecurityOptions = []
      dockerLabels          = {}
      ulimits               = []
      logConfiguration      = {
        logDriver = "awslogs",
        options   = {
          awslogs-create-group  = "true"
          awslogs-group         = "/ecs/adhoc_${terraform.workspace}_kiosk"
          awslogs-region        = data.aws_region.region.name
          awslogs-stream-prefix = "ecs"
        },
        secretOptions = [
        ]
      },
      systemControls = [
      ]
    }
  ])
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  // TODO
  #task_role_arn            = data.aws_iam_role.ecs_task_execution_role.arn
  execution_role_arn       = data.aws_iam_role.ecs_task_execution_role.arn
  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }
}

resource "aws_ecs_task_definition" "adhoc_server" {
  family                = "adhoc_${terraform.workspace}_server"
  container_definitions = jsonencode([
    {
      name         = "adhoc_${terraform.workspace}_server"
      image        = aws_ecr_repository.adhoc_server.repository_url
      cpu          = 0
      portMappings = [
        {
          name          = "adhoc_${terraform.workspace}_server-8889-tcp"
          containerPort = 8889
          hostPort      = 8889
          protocol      = "tcp"
        }
      ],
      essential = true
      secrets   = [
        {
          name      = "CA_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_ca_certificate"
        },
        {
          name      = "SERVER_CERTIFICATE"
          valueFrom = "adhoc_${terraform.workspace}_server_certificate"
        },
        {
          name      = "PRIVATE_KEY"
          valueFrom = "adhoc_${terraform.workspace}_private_key"
        },
        {
          name      = "SERVER_BASIC_AUTH_PASSWORD"
          valueFrom = "adhoc_${terraform.workspace}_server_basic_auth_password"
        }
      ],
      logConfiguration = {
        logDriver = "awslogs",
        options   = {
          awslogs-create-group  = "true"
          awslogs-group         = "/ecs/adhoc_${terraform.workspace}_server"
          awslogs-region        = data.aws_region.region.name
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  // TODO
  #task_role_arn            = data.aws_iam_role.ecs_task_execution_role.arn
  execution_role_arn       = data.aws_iam_role.ecs_task_execution_role.arn
  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }
}

resource "aws_ecs_cluster" "adhoc" {
  name = "adhoc_${terraform.workspace}"
}

resource "aws_ecs_cluster_capacity_providers" "adhoc" {
  cluster_name       = aws_ecs_cluster.adhoc.name
  capacity_providers = ["FARGATE_SPOT"]

  default_capacity_provider_strategy {
    base              = 0
    weight            = 100
    capacity_provider = "FARGATE_SPOT"
  }
}

resource "aws_ecs_service" "adhoc_manager" {
  name                               = "adhoc_${terraform.workspace}_manager"
  cluster                            = aws_ecs_cluster.adhoc.id
  #launch_type     = "FARGATE"
  task_definition                    = "${aws_ecs_task_definition.adhoc_manager.family}:${aws_ecs_task_definition.adhoc_manager.revision}"
  desired_count                      = 0
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  capacity_provider_strategy {
    base              = 0
    weight            = 100
    capacity_provider = "FARGATE_SPOT"
  }
  network_configuration {
    security_groups  = [aws_security_group.adhoc_manager.id]
    subnets          = [aws_subnet.adhoc_a.id]
    assign_public_ip = true
  }
  service_registries {
    registry_arn = aws_service_discovery_service.adhoc_manager.arn
  }
  lifecycle {
    ignore_changes = [desired_count]
  }
}

resource "aws_ecs_service" "adhoc_kiosk" {
  name                               = "adhoc_${terraform.workspace}_kiosk"
  cluster                            = aws_ecs_cluster.adhoc.id
  #launch_type = "FARGATE"
  task_definition                    = "${aws_ecs_task_definition.adhoc_kiosk.family}:${aws_ecs_task_definition.adhoc_kiosk.revision}"
  desired_count                      = 0
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  capacity_provider_strategy {
    base              = 0
    weight            = 100
    capacity_provider = "FARGATE_SPOT"
  }
  network_configuration {
    security_groups  = [aws_security_group.adhoc_kiosk.id]
    subnets          = [aws_subnet.adhoc_a.id]
    assign_public_ip = true
  }
  service_registries {
    registry_arn = aws_service_discovery_service.adhoc_kiosk.arn
  }
  lifecycle {
    ignore_changes = [desired_count]
  }
}