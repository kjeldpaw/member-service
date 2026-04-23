package dk.wandywharang;

import dk.wandywharang.api.record.BeltRecord;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.service.BeltService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/belts")
@RequestScoped
public class BeltResource {
    private final BeltService beltService;
    private final BeltMapper beltMapper;

    public BeltResource(BeltService beltService,
                        @SuppressWarnings("CdiInjectionPointsInspection") BeltMapper beltMapper) {
        this.beltService = beltService;
        this.beltMapper = beltMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Uni<List<BeltRecord>> findAll() {
        return beltService.findAll()
                .map(list -> list.stream().map(beltMapper::toRecord).toList());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("member")
    public Uni<BeltRecord> findById(@PathParam("id") UUID id) {
        return beltService.findById(id)
                .map(beltMapper::toRecord);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    @ResponseStatus(201)
    public Uni<BeltRecord> create(@Valid BeltRecord belt) {
        return beltService.create(belt)
                .map(beltMapper::toRecord);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("admin")
    public Uni<Void> delete(@PathParam("id") UUID id) {
        return beltService.delete(id);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("admin")
    public Uni<Void> update(@PathParam("id") UUID id, @Valid BeltRecord belt) {
        if (id.equals(belt.getId())) {
            return beltService.update(id, belt);
        } else {
            return Uni.createFrom().failure(new BadRequestException("Belt id does not match path parameter"));
        }
    }
}
