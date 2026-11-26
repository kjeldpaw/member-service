package dk.wandywharang;

import dk.wandywharang.api.Examiner;
import dk.wandywharang.service.ExaminerService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Looks up Examiners for a Graduation: Members of any Club, found by name. Only their names are exposed, as an
 * Instructor may pick an Examiner from a Club whose Members they may not otherwise see.
 */
@Path("/api/v1/examiners")
@RequestScoped
@RequiredArgsConstructor
public class ExaminerResource {
    private final ExaminerService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    public Uni<List<Examiner>> search(@QueryParam("q") String query) {
        return service.search(query);
    }
}
