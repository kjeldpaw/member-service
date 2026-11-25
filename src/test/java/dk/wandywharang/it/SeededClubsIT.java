package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Lists the Clubs that changeset 1.0.0 seeds, against a database that Liquibase has just migrated and no other test
 * has written to (see {@link FreshDatabaseProfile}). The frontend, tests and support queries rely on these fixed ids.
 */
@QuarkusTest
@TestProfile(SeededClubsIT.FreshDatabaseProfile.class)
@Tag("integration")
class SeededClubsIT {

    private static final Map<String, String> SEEDED_CLUBS = Map.of(
            "0d09a35b-02c4-4279-9416-a5a302613f3f", "Randers Taekwondo Klub",
            "a4f3fcc3-904b-48c0-9b2b-4225dc6dbe92", "Midtdjurs Taekwondo Klub",
            "eced5ba8-4097-4284-b69b-4c8ef24838cc", "Taekwondoklubben Hwa Rang Aarhus",
            "e52a3058-c086-4588-b7cf-4a30f14a10a1", "Nordjysk Taekwondo Center");

    @Test
    void memberListsExactlyTheFourSeededClubsWithTheirFixedIds() {
        given()
                .auth().oauth2(TestRealm.MEMBER_A.token())
                .get("/api/v1/clubs")
                .then().statusCode(200)
                .body("id", containsInAnyOrder(SEEDED_CLUBS.keySet().toArray()))
                .body("name", containsInAnyOrder(SEEDED_CLUBS.values().toArray()));
    }

    @Test
    void memberFetchesEachSeededClubByItsFixedId() {
        final var token = TestRealm.MEMBER_A.token();
        SEEDED_CLUBS.forEach((id, name) -> given()
                .auth().oauth2(token)
                .get("/api/v1/clubs/{id}", id)
                .then().statusCode(200)
                .body("id", equalTo(id))
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
