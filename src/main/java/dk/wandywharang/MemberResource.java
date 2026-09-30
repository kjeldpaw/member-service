package dk.wandywharang;

import dk.wandywharang.api.*;
import dk.wandywharang.service.MemberService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/members")
@RequestScoped
@RequiredArgsConstructor
public class MemberResource {
    private final MemberService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<List<Member>> findAll() {
        return service.findAll();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<Member> findById(@PathParam("id") UUID id) {
        return service.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @ResponseStatus(201)
    public Uni<Member> create(@Valid CreateMemberRequest request) {
        return service.create(request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}")
    public Uni<Member> update(@PathParam("id") UUID id, @Valid UpdateMemberRequest request) {
        return service.update(id, request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference")
    @ResponseStatus(201)
    public Uni<Member> createReference(@PathParam("id") UUID id, @Valid CreateReferenceRequest request) {
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference/{referenceId}")
    public Uni<Member> updateReference(@PathParam("id") UUID id, @PathParam("referenceId") UUID referenceId, @Valid UpdateReferenceRequest reference) {
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference/{referenceId}")
    public Uni<Member> deleteReference(@PathParam("id") UUID id, @PathParam("referenceId") UUID referenceId) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation")
    @ResponseStatus(201)
    public Uni<Member> createGraduation(@PathParam("id") UUID id, @Valid CreateGraduationRequest request) {
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation/{graduationId}")
    public Uni<Member> updateReference(@PathParam("id") UUID id, @PathParam("graduationId") UUID graduationId, @Valid Reference reference) {
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation/{graduationId}")
    public Uni<Member> deleteGraduation(@PathParam("id") UUID id, @PathParam("graduationId") UUID graduationId) {
    }


}
