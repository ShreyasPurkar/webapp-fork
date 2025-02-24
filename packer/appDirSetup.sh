#!/bin/bash

# Exit immediately if a command fails
set -e  

# Logging function
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Create a non-login user and group for the application
log "Creating user and group csye6225..."
sudo groupadd --system csye6225 || true
sudo useradd --system --no-create-home --shell /usr/sbin/nologin --gid csye6225 csye6225 || true

# Create application directory
log "Creating application directory..."
sudo mkdir -p /opt/csye6225/webapp

# Move webapp.service file to systemd directory
log "Moving systemd service file..."
sudo cp /tmp/webapp.service /etc/systemd/system/webapp.service

# Set proper permissions
log "Setting permissions for the service file..."
sudo chown -R csye6225:csye6225 /etc/systemd/system/webapp.service
sudo chmod 750 /etc/systemd/system/webapp.service

# Move the JAR file to the application directory
log "Moving JAR file to application directory..."
sudo chown ubuntu:ubuntu /opt/csye6225/webapp
unzip -o /tmp/webapp.zip -d /opt/csye6225/webapp
# sudo cp /tmp/webapp-0.0.1-SNAPSHOT.jar /opt/csye6225/webapp/webapp-0.0.1-SNAPSHOT.jar