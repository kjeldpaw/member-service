package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static dk.wandywharang.it.TestRealm.CLUB_C;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Adding a Club to, and removing one from, an existing Member over HTTP, against real Postgres and Keycloak. See
 * {@link MemberLifecycleIT} for how the integration tests run.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class MemberClubsIT {

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    @Test
    void instructorCanAddMemberWhoSharesNoClubToOneOfTheirOwnClubs() throws SQLException {
        final var instructorToken = TestRealm.INSTRUCTOR_AB.token();
        final var member = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_C));

        given()
                .auth().oauth2(instructorToken)
                .put("/api/v1/members/{id}/clubs/{clubId}", member, CLUB_A)
                .then().statusCode(200)
                .body("id", equalTo(member.toString()))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_C.toString()));

        given()
                .auth().oauth2(instructorToken)
                .get("/api/v1/members/{id}", member)
                .then().statusCode(200)
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_C.toString()));
    }

    @Test
    void instructorCanRemoveMemberFromOneOfTheirOwnClubs() throws SQLException {
        final var instructorToken = TestRealm.INSTRUCTOR_AB.token();
        final var member = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_A, CLUB_C));

        given()
                .auth().oauth2(instructorToken)
                .delete("/api/v1/members/{id}/clubs/{clubId}", member, CLUB_A)
                .then().statusCode(200)
                .body("id", equalTo(member.toString()))
                .body("clubs.id", containsInAnyOrder(CLUB_C.toString()));

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .get("/api/v1/members/{id}", member)
                .then().statusCode(200)
                .body("clubs.id", containsInAnyOrder(CLUB_C.toString()));
    }
}
