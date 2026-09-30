package dk.wandywharang;

import dk.wandywharang.api.Club;
import dk.wandywharang.service.club.ClubService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/clubs")
@RequestScoped
@RequiredArgsConstructor
@RolesAllowed({"member", "admin", "instructor"})
public class ClubResource {
    private final ClubService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Uni<List<Club>> findAll() {
        return service.findAll();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("member")
    public Uni<Club> findById(@PathParam("id") UUID id) {
        return service.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Club not found"));
    }

}
