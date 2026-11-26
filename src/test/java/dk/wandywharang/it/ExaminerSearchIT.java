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
import java.util.stream.IntStream;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static dk.wandywharang.it.TestRealm.CLUB_C;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

/**
 * Searching for Examiners by name across all Clubs over HTTP, against real Postgres and Keycloak. See
 * {@link MemberLifecycleIT} for how the integration tests run.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class ExaminerSearchIT {
    private static final String EXAMINERS = "/api/v1/examiners";

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    @Test
    void instructorFindsExaminersInClubsTheyDoNotBelongToAndSeesOnlyTheirNames() throws SQLException {
        final var lastName = uniqueName();
        final var examiner = TestDatabase.insertNewMember(dataSource, "Hana", lastName, Set.of(CLUB_C));

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .queryParam("q", lastName)
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("id", contains(examiner.toString()))
                .body("[0]", equalTo(Map.of("id", examiner.toString(), "firstName", "Hana", "lastName", lastName)));
    }

    @Test
    void adminFindsExaminersInClubsTheyDoNotBelongTo() throws SQLException {
        final var lastName = uniqueName();
        final var examiner = TestDatabase.insertNewMember(dataSource, "Hana", lastName, Set.of(CLUB_C));

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .queryParam("q", lastName)
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("[0]", equalTo(Map.of("id", examiner.toString(), "firstName", "Hana", "lastName", lastName)))
                .body("id", contains(examiner.toString()));
    }

    @Test
    void adminFindsExaminersByFirstLastOrFullNameIgnoringCase()throws SQLException {
        final var lastName = uniqueName();
        final var inA = TestDatabase.insertNewMember(dataSource, "Hana", lastName, Set.of(CLUB_A));
        final var inBC = TestDatabase.insertNewMember(dataSource, "Jun", lastName, Set.of(CLUB_B, CLUB_C));
        final var adminToken = TestRealm.ADMIN.token();

        given()
                .auth().oauth2(adminToken)
                .queryParam("q", lastName.toUpperCase())
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("id", containsInAnyOrder(inA.toString(), inBC.toString()));

        given()
                .auth().oauth2(adminToken)
                .queryParam("q", "jun " + lastName.substring(0, 10).toLowerCase())
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("id", contains(inBC.toString()));
    }

    @Test
    void plainMemberIsForbiddenAndAnonymousCallerIsUnauthorised() {
        given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .queryParam("q", "Test")
                .get(EXAMINERS)
                .then().statusCode(403);

        given()
                .queryParam("q", "Test")
                .get(EXAMINERS)
                .then().statusCode(401);
    }

    @Test
    void missingOrShorterThanTwoCharacterQueryIsABadRequest() {
        final var instructorToken = TestRealm.INSTRUCTOR_AB.token();

        given()
                .auth().oauth2(instructorToken)
                .get(EXAMINERS)
                .then().statusCode(400)
                .contentType("application/json")
                .body("message", equalTo("Query q must be at least 2 characters"));

        for (final var query : new String[]{"", "T", " T  "}) {
            given()
                    .auth().oauth2(instructorToken)
                    .queryParam("q", query)
                    .get(EXAMINERS)
                    .then().statusCode(400);
        }
    }

    @Test
    void resultsAreOrderedByNameAndCappedAtTwenty() throws SQLException {
        final var lastName = uniqueName();
        for (int i = 25; i >= 1; i--) {
            TestDatabase.insertNewMember(dataSource, String.format("Kim%02d", i), lastName, Set.of(CLUB_A));
        }
        final var firstTwenty = IntStream.rangeClosed(1, 20).mapToObj(i -> String.format("Kim%02d", i)).toArray();

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_C.token())
                .queryParam("q", lastName)
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("firstName", contains(firstTwenty));
    }

    @Test
    void wildcardCharactersInTheQueryMatchOnlyThemselves() throws SQLException {
        final var lastName = uniqueName();
        final var underscored = TestDatabase.insertNewMember(dataSource, "Hana", lastName + "_x", Set.of(CLUB_A));
        TestDatabase.insertNewMember(dataSource, "Hana", lastName + "yx", Set.of(CLUB_A));
        final var instructorToken = TestRealm.INSTRUCTOR_AB.token();

        given()
                .auth().oauth2(instructorToken)
                .queryParam("q", "%%")
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("$", empty());

        given()
                .auth().oauth2(instructorToken)
                .queryParam("q", lastName + "_x")
                .get(EXAMINERS)
                .then().statusCode(200)
                .body("id", contains(underscored.toString()));
    }

    /**
     * A last name no other test uses, so a search for it finds only the Members a test inserts.
     */
    private static String uniqueName() {
        return "Examiner" + Long.toString(System.nanoTime(), 36);
    }
}
