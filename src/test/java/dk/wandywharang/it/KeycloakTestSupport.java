package dk.wandywharang.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.admin.client.Keycloak;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Obtains real access tokens from the Keycloak Dev Services container for the users defined in
 * wandywharang-realm.json, so integration tests exercise the actual OIDC flow instead of {@code @TestSecurity}.
 */
public final class KeycloakTestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CLIENT_ID = "member-service";
    private static final String CLIENT_SECRET = "member-service-secret";

    private KeycloakTestSupport() {
    }

    public static String tokenFor(String username, String password) {
        final var authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", CLIENT_ID)
                .formParam("client_secret", CLIENT_SECRET)
                .formParam("username", username)
                .formParam("password", password)
                .when().post(authServerUrl + "/protocol/openid-connect/token")
                .then().statusCode(200)
                .extract().path("access_token");
    }

    /**
     * Asks Keycloak, through the application's admin client, whether a user with exactly this email exists in the
     * realm that Members are registered in.
     */
    public static boolean userExists(String email) {
        final var realm = ConfigProvider.getConfig().getValue("member-service.keycloak.realm", String.class);
        final var keycloak = Arc.container().instance(Keycloak.class).get();
        return !keycloak.realm(realm).users().searchByEmail(email, true).isEmpty();
    }

    /**
     * Decodes the token's {@code sub} claim, which is the Keycloak user id that the application uses as the
     * principal name (see {@code quarkus.oidc.token.principal-claim=sub}) and therefore as the member id.
     */
    public static UUID subjectOf(String token) {
        final var payload = token.split("\\.")[1];
        final var json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        try {
            @SuppressWarnings("unchecked")
            final var claims = MAPPER.readValue(json, Map.class);
            return UUID.fromString((String) claims.get("sub"));
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse token subject", e);
        }
    }
}
