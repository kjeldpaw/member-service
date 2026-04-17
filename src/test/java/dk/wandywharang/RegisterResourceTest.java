package dk.wandywharang;

import dk.wandywharang.api.RegisterRecord;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class RegisterResourceTest {

    @Test
    void testRegister() {
        final var register = new RegisterRecord("John", "Doe", "john.doe@example.com", "password123");

        given()
                .contentType(ContentType.JSON)
                .body(register)
                .when()
                .post("/api/v1/register")
                .then()
                .statusCode(201);
    }

    @Test
    void testRegisterAlreadyExists() {
        final var register = new RegisterRecord("Jane", "Doe", "jane.doe@example.com", "password123");

        // First registration
        given()
                .contentType(ContentType.JSON)
                .body(register)
                .when()
                .post("/api/v1/register")
                .then()
                .statusCode(201);

        // Second registration with same email
        given()
                .contentType(ContentType.JSON)
                .body(register)
                .when()
                .post("/api/v1/register")
                .then()
                .statusCode(409);
    }



}
