# webapp

## Description
A health check API to monitor the health of the application instance.

## Prerequisites
Before building or running the project, make sure you have the following installed:
- **Java 17+** (or your project-specific version)
- **Maven 3.8+** (or your build tool)
- Any additional tools or dependencies.

To check the versions installed:
```bash
  java -version
  mvn -version
```
## Build Instructions
### 1. Clone the Repository
```angular2html
git clone git@github.com:NortheasternUniversity-CSYE6225/webapp.git
```
### 2. Set Up Environment Variables

### 3. Run the Application
```bash
  ./run.sh
```

### 4. Run the Postgres server

### 5. Once the application starts, you can access it at:
```angular2html
http://localhost:8080
```
## Testing Instructions
To run API testing suite run
```bash
  ./test.sh
```

## Deployment Instructions
 - Login to the server
 - Upload the webapp.zip file along with deployment script
    ```bash
      scp -i ~/.ssh/do deploy.sh root@<IP-Address>:/tmp
      scp -i ~/.ssh/do webapp.zip root@<IP-Address>:/tmp
   ```
 - Make the deployment script executable
    ```bash
      chmod +x deploy.sh
    ```
 - Execute the deployment script  
    ```bash
      sudo ./deploy.sh  
    ```

## Troubleshooting
#### If you encounter issues during the build or setup process:
 - Ensure all dependencies are installed.
 - Ensure environment variables are setup.
