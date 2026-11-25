package dk.wandywharang.it;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Builds the requests the integration tests use to register Members over the API.
 */
public final class MemberRequests {

    private MemberRequests() {
    }

    /**
     * A unique email, so each test registers a person that neither the database nor Keycloak knows yet.
     */
    public static String newEmail() {
        return "member-" + UUID.randomUUID() + "@example.com";
    }

    public static Map<String, Object> memberBody(String email, UUID... clubIds) {
        return Map.of(
                "firstName", "Kim",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", email,
                "clubIds", Set.of(clubIds));
    }

    /**
     * Registers a Member over the API as an Admin, so the Member also has a Keycloak user.
     *
     * @return the new Member's id
     */
    public static String register(String email, UUID... clubIds) {
        return given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .contentType("application/json").body(memberBody(email, clubIds))
                .post("/api/v1/members")
                .then().statusCode(201)
                .extract().path("id");
    }
}
