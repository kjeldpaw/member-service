package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Lists the Clubs that changeset 1.0.0 seeds, against a database that Liquibase has just migrated and no other test
 * has written to (see {@link FreshDatabaseProfile}). The frontend, tests and support queries rely on these fixed ids.
 *
 * <p>The requests use {@link TestRealm#MEMBER_A}'s login, which has the {@code member} role but, on this fresh
 * database, no Member record. Any logged-in person can read the Clubs, whether or not they are a Member.
 */
@QuarkusTest
@TestProfile(SeededClubsIT.FreshDatabaseProfile.class)
@Tag("integration")
class SeededClubsIT {

    @Test
    void anyLoggedInPersonListsExactlyTheFourSeededClubsWithTheirFixedIds() {
        given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .get("/api/v1/clubs")
                .then().statusCode(200)
                .body("id", containsInAnyOrder(SeededClubs.NAMES.keySet().stream().map(UUID::toString).toArray()))
                .body("name", containsInAnyOrder(SeededClubs.NAMES.values().toArray()));
    }

    @Test
    void anyLoggedInPersonFetchesEachSeededClubByItsFixedId() {
        final var token = TestRealm.MEMBER_A.token();
        SeededClubs.NAMES.forEach((id, name) -> given()
                .auth().oauth2(token)
                .get("/api/v1/clubs/{id}", id)
                .then().statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("name", equalTo(name)));
    }

    /**
     * The other integration tests share one Postgres container and add Clubs of their own. Giving this test's
     * database another name makes Dev Services start a container of its own, which Liquibase migrates from empty.
     */
    public static class FreshDatabaseProfile extends IntegrationTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.devservices.db-name", "seeded_clubs");
        }
    }
}
