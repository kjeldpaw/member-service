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

import static io.restassured.RestAssured.given;
import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static dk.wandywharang.it.TestRealm.CLUB_C;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
        clubId = TestDatabase.insertClub(dataSource, "Integration Test Club " + UUID.randomUUID());
    }

    private static Map<String, Object> memberBody(String email, UUID... clubIds) {
        return Map.of(
                "firstName", "Kim",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", email,
                "clubIds", Set.of(clubIds));
    }

    private static String newEmail() {
        return "member-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void adminCanRegisterMemberIntoSeveralClubsTheyDoNotBelongTo() {
        final var adminToken = TestRealm.ADMIN.token();
        final var email = newEmail();

        final String memberId = given()
                .auth().oauth2(adminToken)
                .contentType("application/json").body(memberBody(email, CLUB_A, CLUB_C))
                .post("/api/v1/members")
                .then().statusCode(201)
                .body("email", equalTo(email))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_C.toString()))
                .extract().path("id");

        given()
                .auth().oauth2(adminToken)
                .get("/api/v1/members/{id}", memberId)
                .then().statusCode(200)
                .body("firstName", equalTo("Kim"))
                .body("email", equalTo(email))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_C.toString()));
    }

    @Test
    void instructorCanRegisterMemberIntoSeveralOfTheirOwnClubs() {
        final var email = newEmail();

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .contentType("application/json").body(memberBody(email, CLUB_A, CLUB_B))
                .post("/api/v1/members")
                .then().statusCode(201)
                .body("email", equalTo(email))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString(), CLUB_B.toString()));
    }

    @Test
    void instructorIsRefusedWhenAnyRequestedClubIsNotTheirsAndNothingIsCreatedInKeycloak() {
        final var email = newEmail();

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .contentType("application/json").body(memberBody(email, CLUB_A, CLUB_C))
                .post("/api/v1/members")
                .then().statusCode(403);

        assertFalse(KeycloakTestSupport.userExists(email));

        // The same email can still be registered, and then the check does find it
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(memberBody(email, CLUB_A, CLUB_C))
                .post("/api/v1/members")
                .then().statusCode(201);
        assertTrue(KeycloakTestSupport.userExists(email));
    }

    @Test
    void adminRegisteringIntoAnUnknownClubIsNotFoundAndNothingIsCreatedInKeycloak() {
        final var email = newEmail();

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(memberBody(email, CLUB_A, UUID.randomUUID()))
                .post("/api/v1/members")
                .then().statusCode(404);

        assertFalse(KeycloakTestSupport.userExists(email));
    }

    @Test
    void registeringWithoutClubsIsABadRequest() {
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(memberBody(newEmail()))
                .post("/api/v1/members")
                .then().statusCode(400);
    }

    @Test
    void registeringAnExistingEmailIsAConflictThatSaysThePersonAlreadyExists() {
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(memberBody(TestRealm.MEMBER_A.username(), CLUB_B))
                .post("/api/v1/members")
                .then().statusCode(409)
                .contentType("application/json")
                .body("message", equalTo("A person with email " + TestRealm.MEMBER_A.username() + " already exists"));
    }

    @Test
    void creatingMemberWithoutAuthenticationIsUnauthorized() {
        given()
                .contentType("application/json")
                .body(memberBody(newEmail(), CLUB_A))
                .post("/api/v1/members")
                .then().statusCode(401);
    }

    @Test
    void memberSeesMembersOfEveryClubTheyBelongToAndNotOthers() throws SQLException {
        final var memberToken = TestRealm.MEMBER_BC.token();
        final var inClubB = TestDatabase.insertNewMember(dataSource, "In", "B", Set.of(CLUB_B));
        final var inClubC = TestDatabase.insertNewMember(dataSource, "In", "C", Set.of(CLUB_C));
        final var inClubsAAndC = TestDatabase.insertNewMember(dataSource, "In", "AC", Set.of(CLUB_A, CLUB_C));
        final var onlyInClubA = TestDatabase.insertNewMember(dataSource, "Only", "A", Set.of(CLUB_A));

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/members")
                .then().statusCode(200)
                .body("id", hasItems(TestRealm.MEMBER_BC.id().toString(), inClubB.toString(), inClubC.toString(),
                        inClubsAAndC.toString(), TestRealm.INSTRUCTOR_AB.id().toString()))
                .body("id", not(hasItem(onlyInClubA.toString())))
                .body("id", not(hasItem(TestRealm.MEMBER_A.id().toString())));

        for (final var visible : List.of(inClubB, inClubC, inClubsAAndC)) {
            given()
                    .auth().oauth2(memberToken)
                    .get("/api/v1/members/{id}", visible)
                    .then().statusCode(200)
                    .body("id", equalTo(visible.toString()));
        }

        given()
                .auth().oauth2(memberToken)
                .get("/api/v1/members/{id}", onlyInClubA)
                .then().statusCode(404);
    }

    @Test
    void membersProfileListsAllTheirClubs() {
        final var member = TestRealm.MEMBER_BC;

        given()
                .auth().oauth2(member.token())
                .get("/api/v1/members/{id}", member.id())
                .then().statusCode(200)
                .body("clubs.id", containsInAnyOrder(CLUB_B.toString(), CLUB_C.toString()))
                .body("clubs.name", containsInAnyOrder("Test Club B", "Test Club C"));
    }

    @Test
    void instructorCanEditMemberWhoSharesAClubButNotOneWhoSharesNone() throws SQLException {
        final var instructorToken = TestRealm.INSTRUCTOR_AB.token();
        final var sharesClubB = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_B, CLUB_C));
        final var sharesNone = TestDatabase.insertNewMember(dataSource, "Sam", "Doe", Set.of(CLUB_C));

        final var updateBody = Map.of(
                "firstName", "Updated",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", "kim@example.com");

        given()
                .auth().oauth2(instructorToken)
                .contentType("application/json").body(updateBody)
                .put("/api/v1/members/{id}", sharesClubB)
                .then().statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("clubs.id", containsInAnyOrder(CLUB_B.toString(), CLUB_C.toString()));

        given()
                .auth().oauth2(instructorToken)
                .contentType("application/json").body(updateBody)
                .put("/api/v1/members/{id}", sharesNone)
                .then().statusCode(403);
    }

    @Test
    void instructorCanRecordGraduationForMemberWhoSharesAClubButNotOneWhoSharesNone() throws SQLException {
        final var instructor = TestRealm.INSTRUCTOR_C;
        final var beltId = TestDatabase.insertBelt(dataSource, "Test Belt " + UUID.randomUUID(), 100);
        final var sharesClubC = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_A, CLUB_C));
        final var sharesNone = TestDatabase.insertNewMember(dataSource, "Sam", "Doe", Set.of(CLUB_A, CLUB_B));
        final var graduation = Map.of(
                "date", "2026-06-01",
                "graduatedBy", Set.of(instructor.id()),
                "beltId", beltId);

        given()
                .auth().oauth2(instructor.token())
                .contentType("application/json").body(graduation)
                .post("/api/v1/members/{id}/graduation", sharesClubC)
                .then().statusCode(201)
                .body("graduation.belt.id", equalTo(beltId.toString()));

        given()
                .auth().oauth2(instructor.token())
                .contentType("application/json").body(graduation)
                .post("/api/v1/members/{id}/graduation", sharesNone)
                .then().statusCode(403);
    }

    @Test
    void healthEndpointsReportUpWithRealDependencies() {
        given().get("/q/health/live")
                .then().statusCode(200)
                .body("status", equalTo("UP"));

        given().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
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
