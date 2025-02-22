packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0, < 2.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-2"
}

variable "instance_type" {
  type    = string
  default = "t2.micro"
}

variable "db_name" {
  type    = string
  default = "webapp"
}

variable "db_username" {
  type    = string
  default = "postgres"
}

variable "db_password" {
  type    = string
  default = "password"
}

source "amazon-ebs" "webapp" {
  ami_name        = "webapp-custom-image-{{timestamp}}"
  ami_description = "Custom image for webapp with PostgreSQL"
  instance_type   = var.instance_type
  region          = var.aws_region

  source_ami_filter {
    filters = {
      name                = "ubuntu/images/*ubuntu-noble-24.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["099720109477"]
  }

  ssh_username = "ubuntu"
  ssh_timeout  = "30m"

  aws_polling {
    delay_seconds = 30
    max_attempts  = 50
  }

  ami_users    = []
  encrypt_boot = true
}

build {
  name = "webapp-custom-ami-build"

  sources = [
    "source.amazon-ebs.webapp",
  ]

  provisioner "file" {
    pause_before = "10s"
    source       = "../target/webapp-0.0.1-SNAPSHOT.jar"
    destination  = "/tmp/webapp-0.0.1-SNAPSHOT.jar"
  }

  provisioner "shell" {
    inline = [
      "chmod 755 /tmp/webapp-0.0.1-SNAPSHOT.jar"
    ]
  }

  provisioner "shell" {
    environment_vars = [
      "DB_NAME=${var.db_name}",
      "DB_USERNAME=${var.db_username}",
      "DB_PASSWORD=${var.db_password}"
    ]
    script = "./setup-webapp.sh"
  }
}