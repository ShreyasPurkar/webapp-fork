packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0, < 2.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type        = string
  default     = "us-east-2"
  description = "The AWS region where the EC2 instance will be launched"
}

variable "aws_instance_type" {
  type        = string
  default     = "t2.micro"
  description = "The EC2 instance type"
}

variable "aws_demo_account_id" {
  type        = string
  description = "The demo AWS account ID"
}

variable "aws_source_image" {
  type        = string
  default     = "ubuntu/images/*ubuntu-noble-24.04-amd64-server-*"
  description = "The source image for the EC2 instance"
}

variable "aws_volume_size" {
  type        = number
  default     = 20
  description = "The EBS volume size in GB"
}

variable "aws_volume_type" {
  type        = string
  default     = "gp2"
  description = "The EBS volume type"
}

variable "ssh_username" {
  type        = string
  default     = "ubuntu"
  description = "The SSH username for the instance"
}

variable "db_name" {
  type        = string
  sensitive   = true
  description = "The name of the database"
}

variable "db_username" {
  type        = string
  sensitive   = true
  description = "The username of the database"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "The password of the database"
}

variable "github_workspace" {
  type        = string
  description = "The GitHub workspace directory"
}

# Build the custom AMI for the webapp on AWS
source "amazon-ebs" "webapp" {
  ami_name                = "webapp-custom-image-{{timestamp}}"
  ami_description         = "Custom image for webapp with Java and PostgreSQL"
  instance_type           = var.aws_instance_type
  region                  = var.aws_region
  ssh_username            = var.ssh_username
  ssh_timeout             = "30m"
  ssh_handshake_attempts  = "20"
  pause_before_connecting = "10s"
  ami_users               = [var.aws_demo_account_id]

  source_ami_filter {
    filters = {
      name                = var.aws_source_image
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["099720109477"]
  }

  aws_polling {
    delay_seconds = 30
    max_attempts  = 50
  }

  launch_block_device_mappings {
    delete_on_termination = true
    device_name           = "/dev/sda1"
    volume_size           = var.aws_volume_size
    volume_type           = var.aws_volume_type
  }
}

# Provision the custom AMI for the webapp on AWS
build {
  name = "webapp-custom-ami-build"

  sources = [
    "source.amazon-ebs.webapp"
  ]

  provisioner "file" {
    source      = "./webapp.service"
    destination = "/tmp/webapp.service"
  }

  provisioner "file" {
    pause_before = "15s"
    source       = "${var.github_workspace}/target/webapp.zip"
    destination  = "/tmp/webapp.zip"
    max_retries  = 3
    timeout      = "30m"
  }

  provisioner "file" {
    source      = "./cloudwatch-config.json"
    destination = "/tmp/cloudwatch-config.json"
  }

  provisioner "shell" {
    scripts = [
      "./updateOs.sh",
      "./appDirSetup.sh"
    ]
  }
}