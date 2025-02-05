source ./env.sh

mvn clean install -DskipTests
mvn spring-boot:run