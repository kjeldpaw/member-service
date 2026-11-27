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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static dk.wandywharang.it.TestRealm.CLUB_C;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @ParameterizedTest
    @ValueSource(strings = {"member", "instructor", "admin"})
    void everyRoleWithoutAMemberRecordCanCreateTheirProfileInAnyClubs(String role) {
        final var person = TestRealm.newPerson(role);
        final var token = person.token();

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(createBody(CLUB_B, CLUB_C))
                .post(PROFILE)
                .then().statusCode(201)
                .body("id", equalTo(person.id().toString()))
                .body("firstName", equalTo("Sam"))
                .body("email", equalTo(person.username()))
                .body("clubs.id", containsInAnyOrder(CLUB_B.toString(), CLUB_C.toString()));

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(200)
                .body("id", equalTo(person.id().toString()))
                .body("email", equalTo(person.username()))
                .body("clubs.id", containsInAnyOrder(CLUB_B.toString(), CLUB_C.toString()));
    }

    @Test
    void createIsAConflictForACallerWhoAlreadyHasAMemberRecordAndNothingChanges() throws SQLException {
        final var person = TestRealm.newPerson("member");
        TestDatabase.ensureMember(dataSource, person.id(), "Kim", "Lee", person.username(), Set.of(CLUB_A));
        final var token = person.token();

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(createBody(CLUB_B))
                .post(PROFILE)
                .then().statusCode(409)
                .contentType("application/json")
                .body("message", equalTo("A Member with id = " + person.id() + " already exists"));

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(200)
                .body("firstName", equalTo("Kim"))
                .body("clubs.id", containsInAnyOrder(CLUB_A.toString()));
    }

    @Test
    void createIsAConflictWhenAnotherMemberHasTheCallersEmail() throws SQLException {
        final var person = TestRealm.newPerson("member");
        TestDatabase.ensureMember(dataSource, UUID.randomUUID(), "Kim", "Lee", person.username().toUpperCase(),
                Set.of(CLUB_A));
        final var token = person.token();

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(createBody(CLUB_A))
                .post(PROFILE)
                .then().statusCode(409)
                .contentType("application/json")
                .body("message", equalTo("A person with email " + person.username() + " already exists"));

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(404);
    }

    /**
     * Requests sent at the same moment can all pass the check for an existing Member before any of them is stored, so
     * the database's constraints are what refuse all but one of them.
     */
    @Test
    void concurrentCreatesStoreOneProfileAndTheRestAreConflicts() throws Exception {
        final var person = TestRealm.newPerson("member");
        final var token = person.token();
        final var requests = 8;
        final var start = new CountDownLatch(1);
        final var executor = Executors.newFixedThreadPool(requests);
        try {
            final var statuses = new ArrayList<Future<Integer>>();
            for (var i = 0; i < requests; i++) {
                statuses.add(executor.submit(() -> {
                    start.await();
                    return given()
                            .auth().oauth2(token)
                            .contentType("application/json").body(createBody(CLUB_A))
                            .post(PROFILE)
                            .statusCode();
                }));
            }
            start.countDown();
            final var counts = new HashMap<Integer, Integer>();
            for (final var status : statuses) {
                counts.merge(status.get(30, TimeUnit.SECONDS), 1, Integer::sum);
            }
            assertEquals(Map.of(201, 1, 409, requests - 1), counts);
        } finally {
            executor.shutdownNow();
        }

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(200)
                .body("id", equalTo(person.id().toString()));
    }

    @Test
    void createRejectsAnEmptyClubListAndUnknownClubs() {
        final var token = TestRealm.newPerson("member").token();

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(createBody())
                .post(PROFILE)
                .then().statusCode(400);

        given()
                .auth().oauth2(token)
                .contentType("application/json").body(createBody(CLUB_A, UUID.randomUUID()))
                .post(PROFILE)
                .then().statusCode(404);

        given()
                .auth().oauth2(token)
                .get(PROFILE)
                .then().statusCode(404);
    }

    private static Map<String, Object> createBody(UUID... clubIds) {
        final var body = new HashMap<>(profileBody());
        body.put("clubIds", Arrays.stream(clubIds).map(UUID::toString).toList());
        return body;
    }

    private static Map<String, Object> profileBody() {
        return Map.of(
                "firstName", "Sam",
                "lastName", "Park",
                "address", Map.of("street", "New Street 2", "city", "Randers", "zipCode", "8900"));
    }
}
