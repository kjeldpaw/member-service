package dk.wandywharang;

import dk.wandywharang.api.Member;
import dk.wandywharang.api.MemberRecord;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
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
public class MemberResource {
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final Keycloak keycloak;
    private final SecurityIdentity securityIdentity;
    private final String realm;


    public MemberResource(MemberRepository memberRepository,
                          MemberMapper memberMapper,
                          Keycloak keycloak,
                          SecurityIdentity securityIdentity,
                          @ConfigProperty(name = "quarkus.keycloak.admin-client.realm") String realm) {
        this.memberRepository = memberRepository;
        this.memberMapper = memberMapper;
        this.keycloak = keycloak;
        this.securityIdentity = securityIdentity;
        this.realm = realm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("member")
    public Multi<? extends Member> members() {
        return memberRepository.findAll().list()
                .onItem().transformToMulti(members -> Multi.createFrom().items(members.stream()))
                .onItem().transform(memberMapper::toRecord);
    }



    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTransaction
    @RolesAllowed("member")
    public Uni<? extends Member> createMember(MemberRecord memberRecord) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(memberRecord.email());
        user.setEmail(memberRecord.email());
        user.setFirstName(memberRecord.name());
        user.setEnabled(true);
        user.setCredentials();

        return Uni.createFrom().item(() -> {
            try (Response response = keycloak.realm(realm).users().create(user)) {
                if (response.getStatus() != 201) {
                    throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
                }
                String location = response.getHeaderString("Location");
                return location.substring(location.lastIndexOf("/") + 1);
            }
        }).chain(keycloakId -> {
            MemberEntity entity = memberMapper.toEntity(memberRecord);
            entity.id = UUID.randomUUID();
            entity.keycloakId = keycloakId;
            return memberRepository.persist(entity).map(memberMapper::toRecord);
        });
    }
}
