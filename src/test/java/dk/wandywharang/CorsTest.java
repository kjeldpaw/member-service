package dk.wandywharang;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * The frontend calls the API from its own origin, so the browser sends a CORS preflight first. The allowed origin is
 * configuration, not code: the Helm chart sets it through the QUARKUS_HTTP_CORS_ORIGINS environment variable, so a new
 * frontend host needs no new image. This test sets the same property to an origin of its own to show that.
 */
@QuarkusTest
@TestProfile(CorsTest.FrontendOrigin.class)
class CorsTest {

    static final String FRONTEND = "https://frontend.example";

    public static class FrontendOrigin implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.cors.origins", FRONTEND);
        }
    }

    private static ValidatableResponse preflight(String origin, String method) {
        return given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .options("/api/v1/clubs")
                .then();
    }

    @Test
    void preflightFromTheConfiguredOriginIsAllowed() {
        preflight(FRONTEND, "POST")
                .statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo(FRONTEND))
                .header("Access-Control-Allow-Methods", containsStringIgnoringCase("POST"))
                .header("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization"));
    }

    @Test
    void preflightFromAnotherOriginIsRejected() {
        preflight("https://evil.example", "POST")
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void preflightDoesNotOfferMethodsTheApiDoesNotUse() {
        preflight(FRONTEND, "PATCH")
                .header("Access-Control-Allow-Methods", not(containsStringIgnoringCase("PATCH")));
    }
}
