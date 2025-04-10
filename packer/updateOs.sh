#!/bin/bash

# Exit immediately if a command fails
set -e  

# Logging function
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Update system
log "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install dependencies
log "Installing required packages..."
sudo apt install -y openjdk-17-jdk 
sudo apt install -y maven
sudo apt install -y unzip curl
sudo apt install -y postgresql-client-16
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
sudo apt install -y jq