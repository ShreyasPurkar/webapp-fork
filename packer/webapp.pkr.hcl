packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0, < 2.0.0"
        source  = "github.com/hashicorp/amazon"
    }
    googlecompute = {
      version = ">= 1.0.0, < 2.0.0"
      source  = "github.com/hashicorp/googlecompute"
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

variable "gcp_source_image" {
  type        = string
  default     = "ubuntu-2404-noble-amd64-v20250214"
  description = "The source image for the GCE instance"
}

variable "gcp_source_image_family" {
  type        = string
  default     = "ubuntu-2404-lts-noble"
  description = "The source image family for the GCE instance"
}

variable "gcp_source_image_project_id" {
  type        = list(string)
  default     = ["ubuntu-os-cloud"]
  description = "The source image project ID for the GCE instance"
}

variable "gcp_dev_project_id" {
  type        = string
  description = "The GCP project ID where the GCE instance will be launched"
}

variable "gcp_demo_project_id" {
  type        = string
  description = "The GCP demo project ID"
}

variable "gcp_zone" {
  type        = string
  default     = "us-central1-a"
  description = "The GCE zone where the GCE instance will be launched"
}

variable "gcp_machine_type" {
  type        = string
  default     = "e2-micro"
  description = "The GCE machine type"
}

variable "gcp_disk_size" {
  type        = number
  default     = 20
  description = "The GCE disk size in GB"
}

variable "gcp_disk_type" {
  type        = string
  default     = "pd-standard"
  description = "The GCE disk type"
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

source "googlecompute" "webapp" {
  image_name              = "webapp-custom-image-{{timestamp}}"
  image_description       = "Custom image for webapp with Java and PostgreSQL"
  project_id              = var.gcp_dev_project_id
  source_image            = var.gcp_source_image
  source_image_family     = var.gcp_source_image_family
  disk_size               = var.gcp_disk_size
  disk_type               = var.gcp_disk_type
  machine_type            = var.gcp_machine_type
  zone                    = var.gcp_zone
  ssh_username            = var.ssh_username
  image_storage_locations = ["us"]
}

build {
  name = "webapp-custom-ami-build"

  sources = [
    "source.amazon-ebs.webapp",
    "source.googlecompute.webapp",
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

  provisioner "shell" {
    environment_vars = [
      "DB_NAME=${var.db_name}",
      "DB_USERNAME=${var.db_username}",
      "DB_PASSWORD=${var.db_password}"
    ]
    scripts = [
      "./updateOs.sh",
      "./setupDatabase.sh",
      "./appDirSetup.sh",
      "./setupWebapp.sh"
    ]
  }
}