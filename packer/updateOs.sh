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
sudo apt install -y postgresql-16 postgresql-contrib-16
sudo apt install -y unzip