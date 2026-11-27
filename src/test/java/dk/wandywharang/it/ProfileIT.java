package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * The logged-in person's own Member record, their Profile, over HTTP, against real Postgres and Keycloak. See
 * {@link MemberLifecycleIT} for how the integration tests run. Each test logs in as a new person, so their Profile
 * starts from scratch.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class ProfileIT {
    private static final String PROFILE = "/api/v1/profile";

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "instructor", "admin"})
    void everyRoleCanGetTheirOwnProfile(String role) throws SQLException {
        final var person = TestRealm.newPerson(role);
        TestDatabase.ensureMember(dataSource, person.id(), "Kim", "Lee", person.username(), Set.of(CLUB_A));

        given()
                .auth().oauth2(person.token())
                .get(PROFILE)
                .then().statusCode(200)
                .body("id", equalTo(person.id().toString()))
                .body("firstName", equalTo("Kim"))
                .body("email", equalTo(person.username()))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "instructor", "admin"})
    void everyRoleCanUpdateTheirOwnProfileButNotTheirEmailOrClubs(String role) throws SQLException {
        final var person = TestRealm.newPerson(role);
        TestDatabase.ensureMember(dataSource, person.id(), "Kim", "Lee", person.username(), Set.of(CLUB_A));
        final var token = person.token();

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(Map.of(
                        "firstName", "Sam",
                        "lastName", "Park",
                        "address", Map.of("street", "New Street 2", "city", "Randers", "zipCode", "8900"),
                        "phone", "12345678",
                        "dateOfBirth", "2001-02-03",
                        "email", "changed-" + person.username(),
                        "clubIds", List.of(CLUB_B.toString())))
                .put(PROFILE)
                .then().statusCode(200)
                .body("id", equalTo(person.id().toString()))
                .body("firstName", equalTo("Sam"))
                .body("lastName", equalTo("Park"))
                .body("address.city", equalTo("Randers"))
                .body("phone", equalTo("12345678"))
                .body("dateOfBirth", equalTo("2001-02-03"))
                .body("email", equalTo(person.username()))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString()));

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(200)
                .body("firstName", equalTo("Sam"))
                .body("address.street", equalTo("New Street 2"))
                .body("email", equalTo(person.username()))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString()));
    }

    @Test
    void updateRejectsAMissingName() throws SQLException {
        final var person = TestRealm.newPerson("member");
        TestDatabase.ensureMember(dataSource, person.id(), "Kim", "Lee", person.username(), Set.of(CLUB_A));

        given()
                .auth().oauth2(person.token())
                .contentType("application/json").body(Map.of(
                        "lastName", "Park",
                        "address", Map.of("street", "New Street 2", "city", "Randers", "zipCode", "8900")))
                .put(PROFILE)
                .then().statusCode(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "instructor", "admin"})
    void getAndUpdateAreNotFoundForACallerWithoutAMemberRecord(String role) {
        final var token = TestRealm.newPerson(role).token();

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(404);

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(profileBody())
                .put(PROFILE)
                .then().statusCode(404);
    }

    private static Map<String, Object> profileBody() {
        return Map.of(
                "firstName", "Sam",
                "lastName", "Park",
                "address", Map.of("street", "New Street 2", "city", "Randers", "zipCode", "8900"));
    }
}
