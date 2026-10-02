package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * End-to-end tests that run the application against real Postgres and Keycloak containers, started by Quarkus Dev
 * Services (backed by Testcontainers) under the "integration" profile - see {@link IntegrationTestProfile}. Unlike
 * the rest of the test suite, nothing here is mocked: requests carry real JWTs issued by Keycloak, and data is
 * persisted to and read back from a real database.
 *
 * <p>Requires Docker to be available. Run with {@code ./gradlew integrationTest}.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class MemberLifecycleIT {

    @Inject
    DataSource dataSource;

    private UUID clubId;

    @BeforeEach
    void seedClub() throws SQLException {
        clubId = TestDatabase.insertClub(dataSource, "Integration Test Club " + UUID.randomUUID());
    }

    private static Map<String, Object> memberBody(String email, UUID clubId) {
        return Map.of(
                "firstName", "Kim",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", email,
                "clubId", clubId);
    }

    @Test
    void adminCanCreateAndRetrieveMember() {
        final var adminToken = KeycloakTestSupport.tokenFor("admin@wandywharang.dk", "admin");
        final var email = "member-" + UUID.randomUUID() + "@example.com";

        final String memberId = given()
                .auth().oauth2(adminToken)
                .contentType("application/json").body(memberBody(email, clubId))
                .post("/api/v1/members")
                .then().statusCode(201)
                .body("email", equalTo(email))
                .extract().path("id");

        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(200)
                .body("firstName", equalTo("Kim"))
                .body("email", equalTo(email));
    }

    @Test
    void creatingMemberWithoutAuthenticationIsUnauthorized() {
        given()
                .contentType("application/json")
                .body(memberBody("anon-" + UUID.randomUUID() + "@example.com", clubId))
                .post("/api/v1/members")
                .then().statusCode(401);
    }

    @Test
    void membersOnlySeeMembersOfTheirOwnClub() throws SQLException {
        final var memberToken = KeycloakTestSupport.tokenFor("member@wandywharang.dk", "member");
        final var memberSubject = KeycloakTestSupport.subjectOf(memberToken);
        TestDatabase.insertMember(dataSource, memberSubject, clubId, "Test", "Member", "member@wandywharang.dk");

        final var otherClubId = TestDatabase.insertClub(dataSource, "Other Club " + UUID.randomUUID());
        final var otherMemberId = UUID.randomUUID();
        TestDatabase.insertMember(dataSource, otherMemberId, otherClubId, "Other", "Person", "other@example.com");

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/members")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(memberSubject.toString()));

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/members/{id}", otherMemberId)
                .then().statusCode(404);
    }

    @Test
    void instructorCanUpdateMemberInOwnClubButNotInAnotherClub() throws SQLException {
        final var instructorToken = KeycloakTestSupport.tokenFor("instructor@wandywharang.dk", "instructor");
        final var instructorSubject = KeycloakTestSupport.subjectOf(instructorToken);
        TestDatabase.insertMember(dataSource, instructorSubject, clubId, "Test", "Instructor", "instructor@wandywharang.dk");

        final var ownClubMemberId = UUID.randomUUID();
        TestDatabase.insertMember(dataSource, ownClubMemberId, clubId, "Kim", "Lee", "kim@example.com");

        final var otherClubId = TestDatabase.insertClub(dataSource, "Other Club " + UUID.randomUUID());
        final var otherClubMemberId = UUID.randomUUID();
        TestDatabase.insertMember(dataSource, otherClubMemberId, otherClubId, "Sam", "Doe", "sam@example.com");

        final var updateBody = Map.of(
                "firstName", "Updated",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", "kim@example.com",
                "clubId", clubId);

        given()
                .auth().oauth2(instructorToken)
                .contentType("application/json").body(updateBody)
                .put("/api/v1/members/{id}", ownClubMemberId)
                .then().statusCode(200)
                .body("firstName", equalTo("Updated"));

        given()
                .auth().oauth2(instructorToken)
                .contentType("application/json").body(updateBody)
                .put("/api/v1/members/{id}", otherClubMemberId)
                .then().statusCode(403);
    }

    @Test
    void clubsAndBeltsAreReadableOnceSeeded() throws SQLException {
        final var beltId = TestDatabase.insertBelt(dataSource, "White Belt", 1);
        final var memberToken = KeycloakTestSupport.tokenFor("member@wandywharang.dk", "member");

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/clubs/{id}", clubId)
                .then().statusCode(200)
                .body("id", equalTo(clubId.toString()));

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/belts/{id}", beltId)
                .then().statusCode(200)
                .body("name", equalTo("White Belt"));
    }
}
