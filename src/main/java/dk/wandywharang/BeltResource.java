package dk.wandywharang;

import dk.wandywharang.api.Belt;
import dk.wandywharang.service.belt.BeltService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/belts")
@RequestScoped
@RequiredArgsConstructor
@RolesAllowed({"member", "admin", "instructor"})
public class BeltResource {
    private final BeltService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Uni<List<Belt>> findAll() {
        return service.findAll();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("member")
    public Uni<Belt> findById(@PathParam("id") UUID id) {
        return service.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Belt not found"));
    }
}
