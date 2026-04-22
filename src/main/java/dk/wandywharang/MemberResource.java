package dk.wandywharang;

import dk.wandywharang.api.record.MemberRecord;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.MemberRepository;
import dk.wandywharang.service.MemberService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.UUID;

@Path("/api/v1/members")
@RequestScoped
public class MemberResource {
    private final MemberService memberService;
    private final SecurityIdentity securityIdentity;
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    private final Keycloak keycloak;

    public MemberResource(MemberService memberService, SecurityIdentity securityIdentity, MemberRepository memberRepository, MemberMapper memberMapper, Keycloak keycloak) {
        this.memberService = memberService;
        this.securityIdentity = securityIdentity;
        this.memberRepository = memberRepository;
        this.memberMapper = memberMapper;
        this.keycloak = keycloak;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Multi<MemberRecord> members() {



        return memberRepository.findAll().list()
                .onItem().transformToMulti(members -> Multi.createFrom().items(members.stream()))
                .onItem().transform(memberMapper::toRecord);
    }



    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTransaction
    @RolesAllowed("member")
    public Uni<MemberRecord> createMember(MemberRecord memberRecord) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(memberRecord.getEmail());
        user.setEmail(memberRecord.getEmail());
        user.setFirstName(memberRecord.getFirstName());
        user.setLastName(memberRecord.getLastName());
        user.setEnabled(true);

        return Uni.createFrom().item(() -> {
            try (Response response = keycloak.realm(realm).users().create(user)) {
                if (response.getStatus() != 201) {
                    throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
                }
                String location = response.getHeaderString("Location");
                return location.substring(location.lastIndexOf("/") + 1);
            }
        }).flatMap(keycloakId -> {
            MemberEntity entity = memberMapper.toEntity(memberRecord);
            entity.setId(keycloakId);
            return memberRepository.persist(entity).map(memberMapper::toRecord);
        });
    }
}
