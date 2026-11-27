package dk.wandywharang;

import dk.wandywharang.api.EligibleMember;
import dk.wandywharang.service.EligibleService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Lists the Members of every Club who are Eligible for a Dan or Poom on the date of a planned Graduation. Instructors
 * see other Clubs' Members here too, but only the few details an {@link EligibleMember} holds (ADR 0002).
 */
@Path("/api/v1/eligible")
@RequestScoped
@RequiredArgsConstructor
public class EligibleResource {
    private static final String DATE = "date";

    private final EligibleService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    public Uni<List<EligibleMember>> findEligible(@QueryParam(DATE) String date) {
        return service.findEligible(parse(date));
    }

    /**
     * The date is taken as text, as JAX-RS turns a query parameter it cannot convert into a 404.
     */
    private static LocalDate parse(String date) {
        try {
            return LocalDate.parse(date == null ? "" : date.strip());
        } catch (DateTimeParseException e) {
            throw new WebApplicationException(ExceptionMappers.error(Response.Status.BAD_REQUEST,
                    String.format("Query %s must be a date as YYYY-MM-DD", DATE)));
        }
    }
}
