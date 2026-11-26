package dk.wandywharang;

import dk.wandywharang.api.ErrorMessage;
import dk.wandywharang.service.AlreadyExistsException;
import dk.wandywharang.service.LastClubException;
import dk.wandywharang.service.QueryTooShortException;
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

    @ServerExceptionMapper
    public Response queryTooShort(QueryTooShortException exception) {
        return error(Response.Status.BAD_REQUEST, exception);
    }

    private static Response conflict(RuntimeException exception) {
        return error(Response.Status.CONFLICT, exception);
    }

    private static Response error(Response.Status status, RuntimeException exception) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorMessage(exception.getMessage()))
                .build();
    }
}
