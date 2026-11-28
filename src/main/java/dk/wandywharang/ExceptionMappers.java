package dk.wandywharang;

import dk.wandywharang.api.ErrorMessage;
import dk.wandywharang.service.AlreadyExistsException;
import dk.wandywharang.service.LastClubException;
import dk.wandywharang.service.RegisterException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Turns the service layer's own exceptions into HTTP responses.
 */
@JBossLog
public class ExceptionMappers {

    @ServerExceptionMapper
    public Response alreadyExists(AlreadyExistsException exception) {
        return conflict(exception);
    }

    @ServerExceptionMapper
    public Response lastClub(LastClubException exception) {
        return conflict(exception);
    }

    /**
     * Logged here because this is the only trace of what Keycloak answered: the caller just sees 502.
     */
    @ServerExceptionMapper
    public Response keycloakFailed(RegisterException exception) {
        log.errorf(exception, "Keycloak admin API failed with status %d: %s",
                exception.keycloakStatus(), exception.getMessage());
        final var message = exception.keycloakStatus() == 0
                ? "Keycloak could not be reached"
                : String.format("Keycloak refused the request (%d)", exception.keycloakStatus());
        return error(Response.Status.BAD_GATEWAY, message);
    }

    private static Response conflict(RuntimeException exception) {
        return error(Response.Status.CONFLICT, exception);
    }

    private static Response error(Response.Status status, RuntimeException exception) {
        return error(status, exception.getMessage());
    }

    static Response error(Response.Status status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorMessage(message))
                .build();
    }
}
