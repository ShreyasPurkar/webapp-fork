#!/bin/bash

# Exit immediately if a command fails
set -e  

# Logging function
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log "Installing PostgreSQL..."
# Install PostgreSQL
sudo apt install -y postgresql-16 postgresql-contrib-16

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