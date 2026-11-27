package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static dk.wandywharang.it.TestRealm.CLUB_B;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The seeded Belts with Poom, and the list of Members Eligible for a Dan or Poom, over HTTP against real Postgres and
 * Keycloak. See {@link MemberLifecycleIT} for how the integration tests run. The database is shared with the other
 * integration tests, so each test looks only at the Members it inserts.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class EligibleIT {
    private static final String ELIGIBLE = "/api/v1/eligible";
    private static final LocalDate GRADUATION_DAY = LocalDate.of(2030, 6, 15);

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    @Test
    void seededBeltsHavePoomBetweenFirstKupAndFirstDanAndFirstKupWaitsAYear() throws SQLException {
        final var belts = given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .get("/api/v1/belts")
                .then().statusCode(200)
                .extract().jsonPath();

        assertEquals(19, rankOf(belts, "Red Belt with three black snippets (1. KUP)"));
        assertEquals(20, rankOf(belts, "Black and red Belt (1. POOM)"));
        assertEquals(21, rankOf(belts, "Black and red Belt (2. POOM)"));
        assertEquals(22, rankOf(belts, "Black and red Belt (3. POOM)"));
        assertEquals(23, rankOf(belts, "Black and red Belt (4. POOM)"));
        assertEquals(24, rankOf(belts, "Black Belt (1. DAN)"));
        assertEquals(32, rankOf(belts, "Black Belt (9. DAN)"));

        final var kup = beltId("Red Belt with three black snippets (1. KUP)");
        final var poom = beltId("Black and red Belt (3. POOM)");
        final var member = TestDatabase.insertNewMember(dataSource, "Hana", "Wait", Set.of(CLUB_A));
        // The waits show in eligibleSince: 1. Kup 365 days, 3. Poom 1096 days
        graduate(member, kup, "2029-01-01");
        assertEquals("2030-01-01", eligible(TestRealm.ADMIN, GRADUATION_DAY).getString(path(member, "eligibleSince")));
        graduate(member, poom, "2026-01-01");
        assertEquals("2029-01-01", eligible(TestRealm.ADMIN, GRADUATION_DAY).getString(path(member, "eligibleSince")));
    }

    @Test
    void instructorGetsEligibleMembersOfEveryClubWithoutTheirContactDetails() throws SQLException {
        final var member = TestDatabase.insertNewMember(dataSource, "Hana", "Kim", Set.of(TestRealm.CLUB_C));
        TestDatabase.setDateOfBirth(dataSource, member, LocalDate.of(1990, 5, 1));
        graduate(member, beltId("Black Belt (1. DAN)"), "2025-03-01");

        final var entry = eligible(TestRealm.INSTRUCTOR_AB, GRADUATION_DAY).<String, Object>getMap(entry(member));

        assertEquals(Set.of("id", "firstName", "lastName", "clubs", "belt", "graduationDate", "nextBelt", "eligibleSince"),
                entry.keySet());
        assertEquals("Hana", entry.get("firstName"));
        assertEquals("Kim", entry.get("lastName"));
        assertEquals("2025-03-01", entry.get("graduationDate"));
        assertEquals("2026-03-01", entry.get("eligibleSince"));
        assertEquals("Black Belt (2. DAN)", ((Map<?, ?>) entry.get("nextBelt")).get("name"));
        assertEquals("Test Club C", ((Map<?, ?>) ((List<?>) entry.get("clubs")).getFirst()).get("name"));
    }

    @Test
    void aWaitEndingUpToOneCalendarMonthAfterTheDateCounts() throws SQLException {
        final var dan = beltId("Black Belt (1. DAN)");
        // 1. Dan waits 365 days: these waits end 2030-07-15, one month after the date, and 2030-07-16
        final var lastDay = TestDatabase.insertNewMember(dataSource, "Hana", "Last", Set.of(CLUB_A));
        final var oneDayLate = TestDatabase.insertNewMember(dataSource, "Hana", "Late", Set.of(CLUB_A));
        graduate(lastDay, dan, "2029-07-15");
        graduate(oneDayLate, dan, "2029-07-16");

        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .queryParam("date", GRADUATION_DAY.toString())
                .get(ELIGIBLE)
                .then().statusCode(200)
                .body("id", hasItem(lastDay.toString()))
                .body("id", not(hasItem(oneDayLate.toString())))
                .body(path(lastDay, "eligibleSince"), equalTo("2030-07-15"));
    }

    @Test
    void underFifteenOnTheDateGoesForAPoomAndWithoutADateOfBirthForADan() throws SQLException {
        final var kup = beltId("Red Belt with three black snippets (1. KUP)");
        final var young = TestDatabase.insertNewMember(dataSource, "Hana", "Young", Set.of(CLUB_B));
        final var unknownAge = TestDatabase.insertNewMember(dataSource, "Hana", "Unknown", Set.of(CLUB_B));
        // Turns 15 a week after the date
        TestDatabase.setDateOfBirth(dataSource, young, GRADUATION_DAY.minusYears(15).plusWeeks(1));
        graduate(young, kup, "2029-01-01");
        graduate(unknownAge, kup, "2029-01-01");

        final var eligible = eligible(TestRealm.INSTRUCTOR_C, GRADUATION_DAY);

        assertEquals("Black and red Belt (1. POOM)", eligible.getString(path(young, "nextBelt.name")));
        assertEquals("Black Belt (1. DAN)", eligible.getString(path(unknownAge, "nextBelt.name")));
    }

    @Test
    void fourthPoomUnderFifteenIsNotEligible() throws SQLException {
        final var member = TestDatabase.insertNewMember(dataSource, "Hana", "Poom", Set.of(CLUB_A));
        TestDatabase.setDateOfBirth(dataSource, member, GRADUATION_DAY.minusYears(14));
        graduate(member, beltId("Black and red Belt (4. POOM)"), "2020-01-01");

        assertNull(eligible(TestRealm.ADMIN, GRADUATION_DAY).get(entry(member)));
    }

    @Test
    void plainMemberIsForbiddenAndAMissingDateIsABadRequest() {
        given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .queryParam("date", GRADUATION_DAY.toString())
                .get(ELIGIBLE)
                .then().statusCode(403);

        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .get(ELIGIBLE)
                .then().statusCode(400)
                .body("message", equalTo("Query date must be a date as YYYY-MM-DD"));

        given()
                .queryParam("date", GRADUATION_DAY.toString())
                .get(ELIGIBLE)
                .then().statusCode(401);
    }

    private static JsonPath eligible(TestRealm.Person person, LocalDate date) {
        return given()
                .auth().oauth2(person.token())
                .queryParam("date", date.toString())
                .get(ELIGIBLE)
                .then().statusCode(200)
                .extract().jsonPath();
    }

    private static String entry(UUID member) {
        return String.format("find { it.id == '%s' }", member);
    }

    private static String path(UUID member, String field) {
        return entry(member) + "." + field;
    }

    private static int rankOf(JsonPath belts, String name) {
        final Integer rank = belts.get(String.format("find { it.name == '%s' }.rank", name));
        assertNotNull(rank, () -> "No Belt named " + name);
        return rank;
    }

    private static UUID beltId(String name) {
        return UUID.fromString(given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .get("/api/v1/belts")
                .then().statusCode(200)
                .extract().jsonPath().getString(String.format("find { it.name == '%s' }.id", name)));
    }

    /**
     * Records a Graduation over the API as an Admin, examined by the Instructor of Clubs A and B.
     */
    private static void graduate(UUID member, UUID belt, String date) {
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json")
                .body(Map.of("date", date, "graduatedBy", Set.of(TestRealm.INSTRUCTOR_AB.id()), "beltId", belt))
                .post("/api/v1/members/{id}/graduation", member)
                .then().statusCode(201);
    }
}
