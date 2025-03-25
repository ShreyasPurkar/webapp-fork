#!/bin/bash

# Exit immediately if a command fails
set -e  

# Logging function
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Create a non-login user and group for the application
log "Creating user and group csye6225..."
sudo groupadd csye6225
sudo useradd --no-create-home --shell /usr/sbin/nologin --gid csye6225 csye6225

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

# Move the cloudwatch config file to the application directory
log "Moving cloudwatch config file to application directory..."
sudo mkdir -p /opt/csye6225/webapp/aws/amazon-cloudwatch-agent/
sudo cp /tmp/cloudwatch-config.json /opt/csye6225/webapp/aws/amazon-cloudwatch-agent/

log "Creating log directory..."
sudo mkdir -p /var/log/csye6225
sudo chown -R csye6225:csye6225 /var/log/csye6225
sudo chmod -R 700 /var/log/csye6225

# Set proper permissions
log "Setting proper permissions..."
sudo chown -R csye6225:csye6225 /opt/csye6225
sudo chmod -R 750 /opt/csye6225