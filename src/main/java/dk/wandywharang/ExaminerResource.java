package dk.wandywharang;

import dk.wandywharang.api.Examiner;
import dk.wandywharang.service.ExaminerService;
import dk.wandywharang.service.QueryTooShortException;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;

/**
 * Looks up Examiners for a Graduation: Members of any Club, found by name. Only their names are exposed, as an
 * Instructor may pick an Examiner from a Club whose Members they may not otherwise see.
 */
@Path("/api/v1/examiners")
@RequestScoped
@RequiredArgsConstructor
public class ExaminerResource {
    private static final String QUERY = "q";

    private final ExaminerService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    public Uni<List<Examiner>> search(@QueryParam(QUERY) String query) {
        return service.search(query);
    }

    @ServerExceptionMapper
    public Response queryTooShort(QueryTooShortException exception) {
        return ExceptionMappers.error(Response.Status.BAD_REQUEST,
                String.format("Query %s must be at least %d characters", QUERY, exception.getMinimumLength()));
    }
}
