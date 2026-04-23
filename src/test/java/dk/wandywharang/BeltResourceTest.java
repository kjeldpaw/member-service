package dk.wandywharang;

import dk.wandywharang.api.record.BeltRecord;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class BeltResourceTest {

    @Test
    @TestSecurity(user = "testUser", roles = {"member"})
    void testFindAll() {
        given()
                .when()
                .get("/api/v1/belts")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testCreateAndFindById() {
        BeltRecord belt = new BeltRecord(null, "White Belt", Duration.ofDays(90), 1);

        BeltRecord createdBelt = given()
                .contentType(ContentType.JSON)
                .body(belt)
                .when()
                .post("/api/v1/belts")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("White Belt"))
                .extract().as(BeltRecord.class);

        given()
                .when()
                .get("/api/v1/belts/" + createdBelt.id())
                .then()
                .statusCode(200)
                .body("id", is(createdBelt.id().toString()))
                .body("name", is("White Belt"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testUpdate() {
        BeltRecord belt = new BeltRecord(null, "Yellow Belt", Duration.ofDays(120), 2);

        BeltRecord createdBelt = given()
                .contentType(ContentType.JSON)
                .body(belt)
                .when()
                .post("/api/v1/belts")
                .then()
                .statusCode(201)
                .extract().as(BeltRecord.class);

        BeltRecord updatedBelt = new BeltRecord(createdBelt.id(), "Yellow Belt Updated", Duration.ofDays(150), 2);

        given()
                .contentType(ContentType.JSON)
                .body(updatedBelt)
                .when()
                .put("/api/v1/belts/" + createdBelt.id())
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v1/belts/" + createdBelt.id())
                .then()
                .statusCode(200)
                .body("name", is("Yellow Belt Updated"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testUpdateIdMismatch() {
        UUID randomId = UUID.randomUUID();
        BeltRecord belt = new BeltRecord(UUID.randomUUID(), "Orange Belt", Duration.ofDays(180), 3);

        given()
                .contentType(ContentType.JSON)
                .body(belt)
                .when()
                .put("/api/v1/belts/" + randomId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testDelete() {
        BeltRecord belt = new BeltRecord(null, "Green Belt", Duration.ofDays(210), 4);

        BeltRecord createdBelt = given()
                .contentType(ContentType.JSON)
                .body(belt)
                .when()
                .post("/api/v1/belts")
                .then()
                .statusCode(201)
                .extract().as(BeltRecord.class);

        given()
                .when()
                .delete("/api/v1/belts/" + createdBelt.id())
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v1/belts/" + createdBelt.id())
                .then()
                .statusCode(204); // findById returns null if not found, Uni<BeltRecord> map(beltMapper::toRecord) -> if findById is Uni<null>, map won't execute or will return null. Quarkus RestEasy Reactive returns 204 for null Uni.
    }

    @Test
    void testUnauthorized() {
        given()
                .when()
                .get("/api/v1/belts")
                .then()
                .statusCode(401);
    }
}
