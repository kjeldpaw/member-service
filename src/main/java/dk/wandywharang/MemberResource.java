package dk.wandywharang;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.service.member.MemberService;
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
    private final SecurityIdentity securityIdentity;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<List<Member>> findAll() {
        if (securityIdentity.hasRole("admin")) {
            return service.findAll();
        } else {
            return profile().onItem().transformToUni(profile -> service.findByClub(profile.club()));
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RolesAllowed({"member", "admin", "instructor"})
    public Uni<Member> findById(@PathParam("id") UUID id) {
        return findById(securityIdentity, id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "instructor"})
    @ResponseStatus(201)
    public Uni<Member> create(@Valid CreateMemberRequest request) {
        return service.create(request);
    }

    private Uni<Member> profile() {
        return service.findById(UUID.fromString(securityIdentity.getPrincipal().getName()))
                .onItem().ifNull().failWith(new NotFoundException(String.format("Profile with id = %s not found", securityIdentity.getPrincipal().getName())));
    }

    public Uni<Member> findById(SecurityIdentity securityIdentity, UUID id) {
        if (securityIdentity.hasRole("admin")) {
            return service.findById(id);
        } else {
            return Uni.combine().all().unis(profile(), service.findById(id))
                    .asTuple().onItem().transform(Unchecked.function(tuple -> {
                        if (tuple.getItem1().club().equals(tuple.getItem2().club())) {
                            return tuple.getItem2();
                        } else {
                            throw new NotFoundException("Member not found");
                        }
                    }));
        }
    }
}
