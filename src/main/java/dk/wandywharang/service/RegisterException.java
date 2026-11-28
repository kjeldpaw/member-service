package dk.wandywharang.service;

/**
 * Keycloak's admin API refused or failed a request that member-service made with its own service account. That is
 * never the caller's fault, so it is answered as 502 Bad Gateway: passing on Keycloak's 401 or 403 would tell the
 * caller that their own login is bad.
 */
public class RegisterException extends RuntimeException {
    private final int keycloakStatus;

    /**
     * @param keycloakStatus the HTTP status Keycloak answered, or 0 when it could not be reached
     */
    public RegisterException(int keycloakStatus, String message) {
        super(message);
        this.keycloakStatus = keycloakStatus;
    }

    public RegisterException(int keycloakStatus, String message, Throwable cause) {
        super(message, cause);
        this.keycloakStatus = keycloakStatus;
    }

    public int keycloakStatus() {
        return keycloakStatus;
    }
}
