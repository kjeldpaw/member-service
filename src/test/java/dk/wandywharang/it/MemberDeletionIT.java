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
import java.util.Set;
import java.util.UUID;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static io.restassured.RestAssured.given;
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
}
