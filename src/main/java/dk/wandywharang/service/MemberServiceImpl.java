package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.ClubRepository;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.List;
import java.util.Set;
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
                    .chain(profile -> repository.findSharingClubWith(profile))
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
     * Keycloak user is removed again on a best-effort basis, so the persist failure is what the caller sees. The setup
     * email is only sent once the member is stored. The Clubs are checked and found before anything is created, so a
     * forbidden request or an unknown Club never reaches Keycloak.
     */
    @WithTransaction
    @Override
    public Uni<Member> create(CreateMemberRequest request) {
        return memberAccess.requireEditableClubs(request.clubIds())
                .chain(() -> findClubs(request.clubIds()))
                .chain(clubs -> registerService.register(request)
                        .chain(userId -> persist(UUID.fromString(userId), request, clubs)
                                .onFailure().call(() -> registerService.unregister(userId)
                                        .onFailure().invoke(e -> log.warnf(e, "Could not remove Keycloak user %s", userId))
                                        .onFailure().recoverWithNull())
                                .call(() -> registerService.sendSetupEmail(userId)
                                        .onFailure().invoke(e -> log.warnf(e, "Could not send setup email to Keycloak user %s", userId))
                                        .onFailure().recoverWithNull())));
    }

    @WithTransaction
    @Override
    public Uni<Member> update(UUID id, UpdateMemberRequest request) {
        return memberAccess.findEditable(id)
                .invoke(member -> mapper.map(request, member))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> addClub(UUID id, UUID clubId) {
        return memberAccess.findForClubChange(id, clubId)
                .chain(member -> findClubs(Set.of(clubId))
                        .invoke(clubs -> member.getClubs().addAll(clubs))
                        .replaceWith(member))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> removeClub(UUID id, UUID clubId) {
        return memberAccess.findForClubChange(id, clubId)
                .call(() -> findClubs(Set.of(clubId)))
                .invoke(Unchecked.consumer(member -> {
                    if (member.clubIds().equals(Set.of(clubId))) {
                        throw new LastClubException(id);
                    }
                    member.getClubs().removeIf(club -> club.getId().equals(clubId));
                }))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    /**
     * The database changes are flushed before the Keycloak user is removed, so a delete that the database refuses
     * never removes the login. If Keycloak fails to remove the login, the request fails and the transaction rolls
     * back, so the Member is never deleted while their login survives.
     */
    @WithTransaction
    @Override
    public Uni<Void> delete(UUID id) {
        return memberAccess.findDeletable(id)
                .call(repository::deleteWithDependents)
                .chain(member -> registerService.unregister(member.getId().toString()));
    }

    private Uni<Set<ClubEntity>> findClubs(Set<UUID> ids) {
        return clubRepository.findByIds(ids).map(clubs -> {
            final var missing = Ids.notIn(ids, clubs.stream().map(ClubEntity::getId).toList());
            if (!missing.isEmpty()) {
                throw new NotFoundException(String.format("Clubs with ids = %s not found", missing));
            }
            return clubs;
        });
    }

    private Uni<Member> persist(UUID id, CreateMemberRequest request, Set<ClubEntity> clubs) {
        final var entity = mapper.map(request);
        entity.setId(id);
        entity.setClubs(clubs);
        return repository.persist(entity).map(mapper::map);
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
