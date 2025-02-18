package com.webapp.shreyas_purkar_002325982.rest.resource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import static io.restassured.RestAssured.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HealthCheckApiTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void healthCheck_Success() {
        given()
        .when()
                .get("/healthz")
        .then()
                .statusCode(201)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }

    @Test
    void healthCheck_MultipleRequests() {
        for (int i = 0; i < 5; i++) {
            given()
            .when()
                    .get("/healthz")
            .then()
                    .statusCode(200)
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache");
        }
    }

    @Test
    void healthCheck_WithTextPayload_ReturnsBadRequest() {
        given()
                .contentType(ContentType.TEXT)
                .body("test payload")
        .when()
                .get("/healthz")
        .then()
                .statusCode(400)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }

    @Test
    void healthCheck_WithQueryParams_ReturnsBadRequest() {
        given()
        .when()
                .get("/healthz?param=value")
        .then()
                .statusCode(400)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }

    @Test
    void healthCheck_WrongHttpMethod_ReturnsMethodNotAllowed() {
        given()
        .when()
                .post("/healthz")
        .then()
                .statusCode(405)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }

    @Test
    void healthCheck_NonExistentEndpoint_ReturnsNotFound() {
        given()
        .when()
                .get("/non-existent")
        .then()
                .statusCode(404)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }



    @Test
    void healthCheck_DatabaseConnectionFailure_ReturnsServiceUnavailable() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }

        given()
        .when()
                .get("/healthz")
        .then()
                .statusCode(503)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");
    }

    @Test
    void healthCheck_WrongDatabasePort_ReturnsServiceUnavailable() {
        System.setProperty("spring.datasource.url",
                "jdbc:postgresql://localhost:5433/webapp");

        given()
        .when()
                .get("/healthz")
        .then()
                .statusCode(503)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");


        System.setProperty("spring.datasource.url",
                "jdbc:postgresql://localhost:5432/webapp");
    }

    @Test
    void healthCheck_DatabaseConnectionTimeout() {
        System.setProperty("spring.datasource.hikari.connection-timeout", "1");

        given()
        .when()
                .get("/healthz")
        .then()
                .statusCode(503)
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache");

        System.setProperty("spring.datasource.hikari.connection-timeout", "30000");
    }
}