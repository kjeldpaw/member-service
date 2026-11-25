package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static dk.wandywharang.it.TestRealm.CLUB_C;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deleting a Member entirely, which only Admins may do, and which also removes the Member's Keycloak login. Runs
 * against real Postgres and Keycloak, like {@link MemberLifecycleIT}.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class MemberDeletionIT {

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    /**
     * Registers a Member over the API as an Admin, so the Member also has a Keycloak user.
     */
    private static String register(String email, UUID... clubIds) {
        final var body = Map.of(
                "firstName", "Kim",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", email,
                "clubIds", Set.of(clubIds));
        return given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(body)
                .post("/api/v1/members")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static String newEmail() {
        return "member-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void adminDeletesMemberAndTheirKeycloakUser() {
        final var email = newEmail();
        final var memberId = register(email, CLUB_A, CLUB_B);
        assertTrue(KeycloakTestSupport.userExists(email));

        final var adminToken = TestRealm.ADMIN.token();
        given()
                .auth().oauth2(adminToken)
                .delete("/api/v1/members/{id}", memberId)
                .then().statusCode(204);

        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(404);

        assertFalse(KeycloakTestSupport.userExists(email));
    }

    @Test
    void adminDeletesMemberWithReferencesAndGraduationsWhoIsAlsoAnExaminer() throws SQLException {
        final var adminToken = TestRealm.ADMIN.token();
        final var email = newEmail();
        final var memberId = register(email, CLUB_A);
        final var otherId = TestDatabase.insertNewMember(dataSource, "Sam", "Doe", Set.of(CLUB_A));
        final var firstBelt = TestDatabase.insertBelt(dataSource, "Test Belt " + UUID.randomUUID(), 100);
        final var secondBelt = TestDatabase.insertBelt(dataSource, "Test Belt " + UUID.randomUUID(), 101);

        given()
                .auth().oauth2(adminToken)
                .contentType("application/json").body(Map.of("type", "KukkiWon", "reference", "K-123"))
                .post("/api/v1/members/{id}/reference", memberId)
                .then().statusCode(201);
        for (final var belt : List.of(firstBelt, secondBelt)) {
            given()
                    .auth().oauth2(adminToken)
                    .contentType("application/json")
                    .body(Map.of("date", "2026-06-01", "graduatedBy", Set.of(otherId), "beltId", belt))
                    .post("/api/v1/members/{id}/graduation", memberId)
                    .then().statusCode(201);
        }
        // The Member to delete examined the other Member, alongside an Instructor
        given()
                .auth().oauth2(adminToken)
                .contentType("application/json")
                .body(Map.of("date", "2026-06-01", "graduatedBy", Set.of(memberId, TestRealm.INSTRUCTOR_AB.id()),
                        "beltId", firstBelt))
                .post("/api/v1/members/{id}/graduation", otherId)
                .then().statusCode(201);

        given()
                .auth().oauth2(adminToken)
                .delete("/api/v1/members/{id}", memberId)
                .then().statusCode(204);

        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(404);
        assertFalse(KeycloakTestSupport.userExists(email));

        // The other Member keeps their Graduation, now examined by the Instructor alone
        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", otherId)
                .then().statusCode(200)
                .body("graduation.belt.id", equalTo(firstBelt.toString()))
                .body("graduation.graduatedBy.id", contains(TestRealm.INSTRUCTOR_AB.id().toString()));
    }

    @Test
    void instructorIsRefusedEvenForMemberWhoSharesAClubAndTheMemberIsUnchanged() {
        final var email = newEmail();
        final var memberId = register(email, CLUB_A, CLUB_B);

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .delete("/api/v1/members/{id}", memberId)
                .then().statusCode(403);

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(200)
                .body("email", equalTo(email))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_B.toString()));
        assertTrue(KeycloakTestSupport.userExists(email));
    }

    @Test
    void memberIsRefused() {
        final var email = newEmail();
        final var memberId = register(email, CLUB_B, CLUB_C);

        given()
                .auth().oauth2(TestRealm.MEMBER_BC.token())
                .delete("/api/v1/members/{id}", memberId)
                .then().statusCode(403);

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(200);
        assertTrue(KeycloakTestSupport.userExists(email));
    }

    @Test
    void deletingAnUnknownMemberIsNotFound() {
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .delete("/api/v1/members/{id}", UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void deletingMemberWhoseKeycloakUserIsAlreadyGoneSucceeds() throws SQLException {
        // Inserted straight into the database, so there is no Keycloak user with this id
        final var memberId = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_A));
        final var adminToken = TestRealm.ADMIN.token();

        given()
                .auth().oauth2(adminToken)
                .delete("/api/v1/members/{id}", memberId)
                .then().statusCode(204);

        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(404);
    }
}
