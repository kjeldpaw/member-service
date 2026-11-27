package dk.wandywharang;

import dk.wandywharang.api.Member;
import dk.wandywharang.service.MemberService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

/**
 * The logged-in person's own Member record, their Profile, open to every role.
 */
@Path("/api/v1/profile")
@RequestScoped
@RequiredArgsConstructor
@RolesAllowed({"member", "admin", "instructor"})
public class ProfileResource {
    private final MemberService memberService;

    /**
     * The caller's Profile, or 404 if they have no Member record.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Member> get() {
        return memberService.findProfile();
    }
}
