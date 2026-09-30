package dk.wandywharang.service.member;

import dk.wandywharang.api.*;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.ClubRepository;
import dk.wandywharang.repository.MemberRepository;
import dk.wandywharang.service.register.RegisterService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MemberServiceImpl implements MemberService {
    private static final Logger LOG = Logger.getLogger(MemberServiceImpl.class);
    private final MemberMapper mapper;
    private final MemberRepository repository;
    private final ClubRepository clubRepository;
    private final RegisterService registerService;

    public MemberServiceImpl(MemberMapper mapper, MemberRepository repository, ClubRepository clubRepository,
                             RegisterService registerService) {
        this.mapper = mapper;
        this.repository = repository;
        this.clubRepository = clubRepository;
        this.registerService = registerService;
    }

    @Override
    public Uni<List<Member>> findAll() {
        return repository.findAll().list()
                .map(entities -> entities.stream().map(mapper::map).toList());
    }

    @Override
    public Uni<Member> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::map);
    }

    @Override
    public Uni<List<Member>> findByClub(Club club) {
        return repository.find("club.id",club.id()).list()
                .map(entities -> entities.stream().map(mapper::map).toList());
    }

    /**
     * Creates the Keycloak user first, so its id can be used as the member id. If persisting the member fails, the
     * Keycloak user is removed again. The setup email is only sent once the member is stored.
     */
    @Override
    public Uni<Member> create(CreateMemberRequest request) {
        return registerService.register(request)
                .chain(userId -> persist(UUID.fromString(userId), request)
                        .onFailure().call(() -> registerService.unregister(userId))
                        .call(() -> registerService.sendSetupEmail(userId)
                                .onFailure().invoke(e -> LOG.warnf(e, "Could not send setup email to Keycloak user %s", userId))
                                .onFailure().recoverWithNull()));
    }

    private Uni<Member> persist(UUID id, CreateMemberRequest request) {
        return Panache.withTransaction(() -> clubRepository.findById(request.clubId())
                .onItem().ifNull().failWith(() -> new NotFoundException(String.format("Club with id = %s not found", request.clubId())))
                .chain(club -> {
                    final var entity = mapper.toEntity(request);
                    entity.setId(id);
                    entity.setClub(club);
                    return repository.persist(entity);
                })
                .map(mapper::map));
    }

}
