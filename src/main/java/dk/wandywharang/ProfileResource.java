package dk.wandywharang;

import dk.wandywharang.api.CreateProfileRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateProfileRequest;
import dk.wandywharang.service.MemberService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

/**
 * The logged-in person's own Member record, their Profile, open to every role.
 */
@Path("/api/v1/profile")
@RequestScoped
@RequiredArgsConstructor
@RolesAllowed({"member", "admin", "instructor"})
public class ProfileResource {
    private final MemberService memberService;
    private final JsonWebToken token;

    /**
     * The caller's Profile, or 404 if they have no Member record.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Member> get() {
        return memberService.findProfile();
    }

    /**
     * Updates the caller's Profile, but not their email or Clubs. 404 if they have no Member record.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Member> update(@Valid UpdateProfileRequest request) {
        return memberService.updateProfile(request);
    }

    /**
     * Creates the caller's Member record, with the token's subject as id and the token's email. No Keycloak user is
     * created and no setup email is sent, as the caller already has a login.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    public Uni<Member> create(@Valid CreateProfileRequest request) {
        final String email = token.getClaim(Claims.email);
        if (email == null || email.isBlank()) {
            throw new BadRequestException("The token has no email");
        }
        return memberService.createProfile(UUID.fromString(token.getSubject()), email, request);
    }
}
