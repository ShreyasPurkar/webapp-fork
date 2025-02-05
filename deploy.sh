#!/bin/bash

APP_DEPLOY_DIR="/opt/csye6225"
APP_ZIP_PATH="/tmp/webapp.zip"
APP_NAME="webapp"

# Exit immediately if a command fails
set -e

# Checks and logs the status of each command
check_status() {
    if [ $? -eq 0 ]; then
        echo "SUCCESS: $1"
    else
        echo "ERROR: $1 failed"
        exit 1
    fi
}

# Checks if a specific command/tool is already installed
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Provides timestamp-based logging
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 1. Update and Upgrade packages
update_system() {
    log "Updating package lists..."
    sudo apt update
    check_status "Package lists update"

    log "Upgrading packages..."
    sudo apt upgrade -y
    check_status "Package upgrade"
}

# 2. Dependencies Installation
install_dependencies() {
    log "Checking and installing dependencies..."

    # Java Installation (skip if already installed)
    if ! command_exists java; then
        log "Installing Java..."
        sudo apt install -y openjdk-17-jdk
        check_status "Java installation"
    else
        log "Java is already installed. Skipping installation."
    fi

    # Maven Installation (skip if already installed)
    if ! command_exists mvn; then
        log "Installing Maven..."
        sudo apt install -y maven
        check_status "Maven installation"
    else
        log "Maven is already installed. Skipping installation."
    fi

    if ! command_exists unzip; then
        log "Installing unzip..."
        sudo apt install -y unzip
        check_status "Unzip installation"
    else
        log "Unzip is already installed. Skipping installation."
    fi

    # PostgresSQL Installation (skip if already installed)
    if ! command_exists psql; then
        log "Installing PostgreSQL..."
        sudo apt install -y postgresql-16 postgresql-contrib-16
        check_status "PostgreSQL installation"
    else
        log "PostgreSQL is already installed. Skipping installation."
    fi
}

# 3. Database Setup
setup_database() {
    log "Starting PostgreSQL service..."
    sudo systemctl start postgresql
    sudo systemctl enable postgresql
    sudo sed -i '/^host/s/ident/md5/' /etc/postgresql/16/main/pg_hba.conf
    sudo sed -i '/^local/s/peer/trust/' /etc/postgresql/16/main/pg_hba.conf
    echo "host all all 0.0.0.0/0 md5" | sudo tee -a /etc/postgresql/16/main/pg_hba.conf
    sudo systemctl restart postgresql
    check_status "PostgreSQL service start"5
}

# 4. Prepare Application Group
prepare_application_group() {
    log "Creating application group..."
    if ! getent group csye6225webapp >/dev/null; then
        sudo groupadd csye6225webapp
        check_status "Group creation"
    fi
}

# 5. Prepare Application User
prepare_application_user() {
    log "Creating application user..."
    if ! id csye6225user >/dev/null 2>&1; then
        sudo useradd -m -d /home/csye6225user -s /bin/false -g csye6225webapp csye6225user
        check_status "User creation"
    fi
}

# 6. Unzip Application
prepare_unzip() {
    log "Preparing to unzip application..."

    # Ensure deployment directory exists and is clean
    mkdir -p "$APP_DEPLOY_DIR"
    check_status "Directory creation"

    # Unzip the application
    log "Unzipping application..."
    unzip -o "$APP_ZIP_PATH" -d "$APP_DEPLOY_DIR"
    check_status "Application file zip"
}

# 7. Set permissions
set_permissions() {
    log "Setting permissions..."
    sudo chown -R csye6225user:csye6225webapp "$APP_DEPLOY_DIR/$APP_NAME"
    sudo chmod -R 750 "$APP_DEPLOY_DIR/$APP_NAME"
    check_status "Permission setup"
}

# Source Environment Variables
source_env() {
    log "Sourcing environment variables..."
    ENV_FILE="$APP_DEPLOY_DIR/$APP_NAME/env.sh"

    # Source the environment file
    source "$ENV_FILE"

    # Validate required environment variables
    REQUIRED_VARS=("DB_NAME" "DB_USERNAME" "DB_PASSWORD")
    for var in "${REQUIRED_VARS[@]}"; do
        if [ -z "${!var}" ]; then
            log "ERROR: $var is not set in the environment file"
            check_status "Environment variable found"
        fi
    done
}

# 8. Create Database
create_database() {
    sudo -u postgres psql -c "ALTER USER postgres PASSWORD '${DB_PASSWORD}';"
    if ! sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw "${DB_NAME}"; then
      sudo -u postgres psql -c "CREATE DATABASE ${DB_NAME};"
      check_status "Database creation"
      log "Database created successfully."
      else
          log "Database ${DB_NAME} already exists. Skipping creation."
      fi
}

# 9. Configure Application
configure_application() {
    log "Configuring application..."

    # Path to properties files
    APP_PROPERTIES="$APP_DEPLOY_DIR/$APP_NAME/src/main/resources/application.properties"
    DB_PROPERTIES="$APP_DEPLOY_DIR/$APP_NAME/src/main/resources/db.properties"

    # Update db.properties
    if [ -f "$DB_PROPERTIES" ]; then
        sed -i "s|\${DB_HOST}|localhost|g" "$DB_PROPERTIES"
        sed -i "s|\${DB_PORT}|5432|g" "$DB_PROPERTIES"
        sed -i "s|\${DB_NAME}|${DB_NAME}|g" "$DB_PROPERTIES"
        sed -i "s|\${DB_USERNAME}|${DB_USERNAME}|g" "$DB_PROPERTIES"
        sed -i "s|\${DB_PASSWORD}|${DB_PASSWORD}|g" "$DB_PROPERTIES"

        log "Updated db.properties with database configuration"
    else
        log "WARNING: db.properties not found"
    fi

    # Update application.properties for APP_NAME
    if [ -f "$APP_PROPERTIES" ]; then
        sed -i "s|\$APP_NAME|$APP_NAME|g" "$APP_PROPERTIES"
        log "Updated application.properties with application name"
    else
        log "WARNING: application.properties not found"
    fi
}

# 10. Build Application
build_application() {
    log "Building application..."
    cd "$APP_DEPLOY_DIR/$APP_NAME"
    sudo -u csye6225user mvn clean package -DskipTests
    check_status "Application build"

    log "Starting application..."
    cd "$APP_DEPLOY_DIR/$APP_NAME/target"
    sudo -u csye6225user java -jar $APP_NAME-0.0.1-SNAPSHOT.jar
    check_status "Application start"
}

# Main Execution
main() {
    update_system
    install_dependencies
    setup_database
    prepare_application_group
    prepare_application_user
    prepare_unzip
    set_permissions
    source_env
    create_database
    configure_application
    build_application
}

# Run the main function
main

exit 0