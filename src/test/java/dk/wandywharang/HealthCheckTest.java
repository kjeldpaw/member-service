package dk.wandywharang;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class HealthCheckTest {

    @Test
    void livenessIsUpEvenWithoutDatabaseOrKeycloak() {
        given().get("/q/health/live")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }
}
