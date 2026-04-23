package dk.wandywharang;

import dk.wandywharang.api.record.BeltRecord;
import dk.wandywharang.api.record.ClubRecord;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.service.belt.BeltService;
import dk.wandywharang.service.club.ClubService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/clubs")
@RequestScoped
public class ClubResource {
    private final ClubService service;
    private final ClubMapper mapper;

    public ClubResource(ClubService service,
                        ClubMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Uni<List<ClubRecord>> findAll() {
        return service.findAll()
                .map(list -> list.stream().map(mapper::toRecord).toList());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("member")
    public Uni<ClubRecord> findById(@PathParam("id") UUID id) {
        return service.findById(id)
                .map(mapper::toRecord);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    @ResponseStatus(201)
    public Uni<ClubRecord> create(@Valid ClubRecord club) {
        return service.create(club)
                .map(mapper::toRecord);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("admin")
    public Uni<Void> delete(@PathParam("id") UUID id) {
        return service.delete(id);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("admin")
    public Uni<Void> update(@PathParam("id") UUID id, @Valid ClubRecord club) {
        if (id.equals(club.getId())) {
            return service.update(id, club);
        } else {
            return Uni.createFrom().failure(new BadRequestException("Club id does not match path parameter"));
        }
    }
}
