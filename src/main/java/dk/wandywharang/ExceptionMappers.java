package dk.wandywharang;

import dk.wandywharang.api.ErrorMessage;
import dk.wandywharang.service.AlreadyExistsException;
import dk.wandywharang.service.LastClubException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Turns the service layer's own exceptions into HTTP responses.
 */
public class ExceptionMappers {

    @ServerExceptionMapper
    public Response alreadyExists(AlreadyExistsException exception) {
        return conflict(exception);
    }

    @ServerExceptionMapper
    public Response lastClub(LastClubException exception) {
        return conflict(exception);
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
