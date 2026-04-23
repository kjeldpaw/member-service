package dk.wandywharang;

import dk.wandywharang.api.record.AddressRecord;
import dk.wandywharang.api.record.ClubRecord;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ClubResourceTest {

    @Test
    @TestSecurity(user = "testUser", roles = {"member"})
    void testFindAll() {
        given()
                .when()
                .get("/api/v1/clubs")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testCreateAndFindById() {
        AddressRecord address = new AddressRecord("Street 1", "City 1", "1234");
        ClubRecord club = new ClubRecord(null, "Test Club", address);

        ClubRecord createdClub = given()
                .contentType(ContentType.JSON)
                .body(club)
                .when()
                .post("/api/v1/clubs")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Test Club"))
                .body("address.street", is("Street 1"))
                .extract().as(ClubRecord.class);

        given()
                .when()
                .get("/api/v1/clubs/" + createdClub.id())
                .then()
                .statusCode(200)
                .body("id", is(createdClub.id().toString()))
                .body("name", is("Test Club"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testUpdate() {
        AddressRecord address = new AddressRecord("Street 2", "City 2", "2345");
        ClubRecord club = new ClubRecord(null, "Update Club", address);

        ClubRecord createdClub = given()
                .contentType(ContentType.JSON)
                .body(club)
                .when()
                .post("/api/v1/clubs")
                .then()
                .statusCode(201)
                .extract().as(ClubRecord.class);

        ClubRecord updatedClub = new ClubRecord(createdClub.id(), "Updated Club Name", address);

        given()
                .contentType(ContentType.JSON)
                .body(updatedClub)
                .when()
                .put("/api/v1/clubs/" + createdClub.id())
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v1/clubs/" + createdClub.id())
                .then()
                .statusCode(200)
                .body("name", is("Updated Club Name"));
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testUpdateIdMismatch() {
        UUID randomId = UUID.randomUUID();
        ClubRecord club = new ClubRecord(UUID.randomUUID(), "Mismatch Club", new AddressRecord("S", "C", "Z"));

        given()
                .contentType(ContentType.JSON)
                .body(club)
                .when()
                .put("/api/v1/clubs/" + randomId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "adminUser", roles = {"admin", "member"})
    void testDelete() {
        ClubRecord club = new ClubRecord(null, "Delete Club", new AddressRecord("S", "C", "Z"));

        ClubRecord createdClub = given()
                .contentType(ContentType.JSON)
                .body(club)
                .when()
                .post("/api/v1/clubs")
                .then()
                .statusCode(201)
                .extract().as(ClubRecord.class);

        given()
                .when()
                .delete("/api/v1/clubs/" + createdClub.id())
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/v1/clubs/" + createdClub.id())
                .then()
                .statusCode(204);
    }

    @Test
    void testUnauthorized() {
        given()
                .when()
                .get("/api/v1/clubs")
                .then()
                .statusCode(401);
    }
}
