package dk.wandywharang.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Creates a user who can log in at once with the password and has the realm role, through the application's
     * admin client, so a test can act as a person without a Member record.
     */
    public static void createUser(String email, String password, String role) {
        final var realm = keycloak().realm(ConfigProvider.getConfig().getValue("member-service.keycloak.realm", String.class));
        final var credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        final var user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName("New");
        user.setLastName("Person");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCredentials(List.of(credential));
        try (final var response = realm.users().create(user)) {
            if (response.getStatus() != 201) {
                throw new IllegalStateException("Could not create user " + email + ": " + response.getStatus());
            }
            final var location = response.getLocation().getPath();
            final var id = location.substring(location.lastIndexOf('/') + 1);
            // Taken from the user's available roles, as the service account may not look up the realm's roles
            final var realmLevel = realm.users().get(id).roles().realmLevel();
            realmLevel.add(realmLevel.listAvailable().stream().filter(available -> available.getName().equals(role)).toList());
        }
    }

    /**
     * The names of the user's effective realm roles, as Keycloak holds them.
     */
    public static Set<String> realmRolesOf(UUID userId) {
        final var realm = ConfigProvider.getConfig().getValue("member-service.keycloak.realm", String.class);
        return keycloak().realm(realm).users().get(userId.toString()).roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

    /**
     * The realm roles that the token carries in its {@code realm_access} claim.
     */
    @SuppressWarnings("unchecked")
    public static Set<String> realmRolesIn(String token) {
        final var realmAccess = (Map<String, Object>) claimsOf(token).get("realm_access");
        return Set.copyOf((List<String>) realmAccess.get("roles"));
    }

    /**
     * The client the application's admin client gets its tokens as.
     */
    public static String adminClientId() {
        return (String) claimsOf(keycloak().tokenManager().getAccessTokenString()).get("azp");
    }

    private static Keycloak keycloak() {
        return Arc.container().instance(Keycloak.class).get();
    }

    /**
     * Decodes the token's {@code sub} claim, which is the Keycloak user id that the application uses as the
     * principal name (see {@code quarkus.oidc.token.principal-claim=sub}) and therefore as the member id.
     */
    public static UUID subjectOf(String token) {
        return UUID.fromString((String) claimsOf(token).get("sub"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> claimsOf(String token) {
        final var payload = token.split("\\.")[1];
        final var json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse token claims", e);
        }
    }
}
