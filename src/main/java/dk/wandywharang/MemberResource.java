package dk.wandywharang;

import dk.wandywharang.api.*;
import dk.wandywharang.service.GraduationService;
import dk.wandywharang.service.MemberService;
import dk.wandywharang.service.ReferenceService;
import io.smallrye.mutiny.Uni;
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
    private final MemberService memberService;
    private final ReferenceService referenceService;
    private final GraduationService graduationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<List<Member>> findAll() {
        return memberService.findAll();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<Member> findById(@PathParam("id") UUID id) {
        return memberService.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @ResponseStatus(201)
    public Uni<Member> create(@Valid CreateMemberRequest request) {
        return memberService.create(request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}")
    public Uni<Member> update(@PathParam("id") UUID id, @Valid UpdateMemberRequest request) {
        return memberService.update(id, request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference")
    @ResponseStatus(201)
    public Uni<Member> createReference(@PathParam("id") UUID id, @Valid CreateReferenceRequest request) {
        return referenceService.create(id, request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference/{referenceId}")
    public Uni<Member> updateReference(@PathParam("id") UUID id, @PathParam("referenceId") UUID referenceId, @Valid UpdateReferenceRequest reference) {
        return referenceService.update(id, referenceId, reference);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/reference/{referenceId}")
    public Uni<Member> deleteReference(@PathParam("id") UUID id, @PathParam("referenceId") UUID referenceId) {
        return referenceService.delete(id, referenceId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation")
    @ResponseStatus(201)
    public Uni<Member> createGraduation(@PathParam("id") UUID id, @Valid CreateGraduationRequest request) {
        return graduationService.create(id, request);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation/{graduationId}")
    public Uni<Member> updateGraduation(@PathParam("id") UUID id, @PathParam("graduationId") UUID graduationId, @Valid UpdateGraduationRequest request) {
        return graduationService.update(id, graduationId, request);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @Path("{id}/graduation/{graduationId}")
    public Uni<Member> deleteGraduation(@PathParam("id") UUID id, @PathParam("graduationId") UUID graduationId) {
        return graduationService.delete(id, graduationId);
    }
}
