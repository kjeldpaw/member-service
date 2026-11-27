package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;

import static dk.wandywharang.it.TestRealm.CLUB_A;
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
}
