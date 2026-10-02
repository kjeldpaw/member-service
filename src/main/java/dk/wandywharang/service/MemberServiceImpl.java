package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.ClubRepository;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.List;
import java.util.UUID;

@JBossLog
@RequestScoped
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberAccess memberAccess;
    private final MemberMapper mapper;
    private final MemberRepository repository;
    private final ClubRepository clubRepository;
    private final RegisterService registerService;

    @WithSession
    @Override
    public Uni<List<Member>> findAll() {
        if (memberAccess.isAdmin()) {
            return repository.findAll().list().chain(this::map);
        } else {
            return memberAccess.profile()
                    .chain(profile -> repository.find("club.id", profile.getClub().getId()).list())
                    .chain(this::map);
        }
    }

    @WithSession
    @Override
    public Uni<Member> findById(UUID id) {
        return memberAccess.findVisible(id)
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    /**
     * Creates the Keycloak user first, so its id can be used as the member id. If persisting the member fails, the
     * Keycloak user is removed again. The setup email is only sent once the member is stored. The club is checked
     * before anything is created, so a forbidden request never reaches Keycloak.
     */
    @WithTransaction
    @Override
    public Uni<Member> create(CreateMemberRequest request) {
        return memberAccess.requireEditableClub(request.clubId())
                .chain(() -> registerService.register(request))
                .chain(userId -> persist(UUID.fromString(userId), request)
                        .onFailure().call(() -> registerService.unregister(userId))
                        .call(() -> registerService.sendSetupEmail(userId)
                                .onFailure().invoke(e -> log.warnf(e, "Could not send setup email to Keycloak user %s", userId))
                                .onFailure().recoverWithNull()));
    }

    @WithTransaction
    @Override
    public Uni<Member> update(UUID id, UpdateMemberRequest request) {
        return memberAccess.findEditable(id)
                .invoke(member -> mapper.map(request, member))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    private Uni<Member> persist(UUID id, CreateMemberRequest request) {
        return clubRepository.findById(request.clubId())
                .onItem().ifNull().failWith(() -> new NotFoundException(String.format("Club with id = %s not found", request.clubId())))
                .chain(club -> {
                    final var entity = mapper.map(request);
                    entity.setId(id);
                    entity.setClub(club);
                    return repository.persist(entity);
                })
                .map(mapper::map);
    }

    /**
     * Fetches the details of the members one at a time, as a reactive session does not allow concurrent use.
     */
    private Uni<List<Member>> map(List<MemberEntity> entities) {
        return Multi.createFrom().iterable(entities)
                .onItem().transformToUniAndConcatenate(repository::fetchDetails)
                .map(mapper::map)
                .collect().asList();
    }
}
