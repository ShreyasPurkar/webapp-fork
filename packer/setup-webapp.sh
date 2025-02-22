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

# Create a non-login user and group for the application
log "Creating user and group csye6225..."
sudo groupadd --system csye6225 || true
sudo useradd --system --no-create-home --shell /usr/sbin/nologin --gid csye6225 csye6225 || true

# Create application directory
log "Creating application directory..."
sudo mkdir -p /opt/csye6225

# Move the JAR file to the application directory
log "Moving JAR file to application directory..."
sudo mv /tmp/webapp-0.0.1-SNAPSHOT.jar /opt/csye6225/

# Create application.properties file with environment variables
log "Creating application.properties..."
sudo tee /opt/csye6225/webapp/application.properties > /dev/null <<EOT
# Application Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
server.port=8080
EOT

# Set proper permissions
log "Setting proper permissions..."
sudo chown -R csye6225:csye6225 /opt/csye6225
sudo chmod -R 750 /opt/csye6225

# Create the systemd service file for the webapp
log "Creating systemd service file..."
sudo tee /etc/systemd/system/webapp.service > /dev/null <<EOT
[Unit]
Description=CSYE6225 Webapp Service
After=network.target postgresql.service

[Service]
User=csye6225
Group=csye6225
WorkingDirectory=/opt/csye6225/webapp
ExecStart=/usr/bin/java -jar /opt/csye6225/webapp-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=webapp

[Install]
WantedBy=multi-user.target
EOT

# Set proper permissions
log "Setting permissions for the service file..."
sudo chmod 644 /etc/systemd/system/webapp.service

# Configure PostgreSQL
log "Configuring PostgreSQL..."
sudo systemctl start postgresql
sudo systemctl enable postgresql
sudo sed -i '/^host/s/ident/md5/' /etc/postgresql/16/main/pg_hba.conf
sudo sed -i '/^local/s/peer/trust/' /etc/postgresql/16/main/pg_hba.conf
echo "host all all 0.0.0.0/0 md5" | sudo tee -a /etc/postgresql/16/main/pg_hba.conf
sudo systemctl restart postgresql

# Setup database
log "Setting up database..."
sudo -u postgres psql -c "ALTER USER ${DB_USERNAME} PASSWORD '${DB_PASSWORD}';"
sudo -u postgres psql -c "CREATE DATABASE ${DB_NAME};"

# Set up systemd service
sudo systemctl daemon-reload
sudo systemctl enable webapp.service

log "Setup complete!"