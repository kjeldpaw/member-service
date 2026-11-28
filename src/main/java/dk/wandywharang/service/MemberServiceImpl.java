package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.CreateProfileRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.Role;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.api.UpdateProfileRequest;
import dk.wandywharang.entity.ClubEntity;
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
        return memberAccess.findAllVisible().chain(this::map);
    }

    @WithSession
    @Override
    public Uni<Member> findById(UUID id) {
        return memberAccess.findVisible(id)
                .chain(repository::fetchDetails)
                .map(mapper::map)
                .chain(this::withRoleForAdmin);
    }

    @WithSession
    @Override
    public Uni<Member> findProfile() {
        return memberAccess.profile()
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> updateProfile(UpdateProfileRequest request) {
        return memberAccess.profile()
                .invoke(member -> mapper.map(request, member))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    /**
     * The caller already has a Keycloak user, so none is created and no setup email is sent.
     */
    @WithTransaction
    @Override
    public Uni<Member> createProfile(CreateProfileRequest request) {
        return memberAccess.caller()
                // Concurrent creates can all pass these checks; the database constraints are the backstop, see persist.
                .call(caller -> repository.findById(caller.id())
                        .onItem().ifNotNull().failWith(() -> new AlreadyExistsException(caller.id())))
                .call(caller -> repository.findByEmail(caller.email())
                        .onItem().ifNotNull().failWith(() -> new AlreadyExistsException(caller.email())))
                .chain(caller -> findClubs(request.clubIds())
                        .chain(clubs -> persist(caller.id(), mapper.map(request, caller.email()), clubs)));
    }

    /**
     * Creates the Keycloak user first, so its id can be used as the member id. If persisting the member fails, the
     * Keycloak user is removed again on a best-effort basis, so the persist failure is what the caller sees. The setup
     * email is only sent once the member is stored. The Role and the Clubs are checked and found before anything is
     * created, so a forbidden request or an unknown Club never reaches Keycloak. A Role above {@link Role#MEMBER} is
     * assigned before the member is stored, and a failure to assign it removes the Keycloak user in the same way.
     */
    @WithTransaction
    @Override
    public Uni<Member> create(CreateMemberRequest request) {
        return memberAccess.requireMayGiveRole(request.role())
                .chain(() -> memberAccess.requireEditableClubs(request.clubIds()))
                .chain(() -> findClubs(request.clubIds()))
                .chain(clubs -> registerService.register(request)
                        .chain(userId -> assignRole(userId, request.role())
                                .chain(() -> persist(UUID.fromString(userId), mapper.map(request), clubs))
                                .onFailure().call(() -> registerService.unregister(userId)
                                        .onFailure().invoke(e -> log.warnf(e, "Could not remove Keycloak user %s", userId))
                                        .onFailure().recoverWithNull())
                                .call(() -> registerService.sendSetupEmail(userId)
                                        .onFailure().invoke(e -> log.warnf(e, "Could not send setup email to Keycloak user %s", userId))
                                        .onFailure().recoverWithNull())))
                .map(member -> memberAccess.isAdmin() ? member.withRole(request.role()) : member);
    }

    @WithTransaction
    @Override
    public Uni<Member> update(UUID id, UpdateMemberRequest request) {
        return memberAccess.findEditable(id)
                .invoke(member -> mapper.map(request, member))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    @WithSession
    @Override
    public Uni<Member> updateRole(UUID id, Role role) {
        return memberAccess.findForRoleChange(id)
                .call(member -> registerService.assignRole(member.getId().toString(), role))
                .chain(repository::fetchDetails)
                .map(member -> mapper.map(member).withRole(role));
    }

    /**
     * The Club is found before the current user's permission for it is checked, so an unknown Club is not found for
     * everyone.
     */
    @WithTransaction
    @Override
    public Uni<Member> addClub(UUID id, UUID clubId) {
        return requireClub(clubId)
                .chain(club -> memberAccess.findForClubChange(id, clubId)
                        .invoke(member -> member.getClubs().add(club)))
                .chain(repository::fetchDetails)
                .map(mapper::map);
    }

    /**
     * The Club is found before the current user's permission for it is checked, so an unknown Club is not found for
     * everyone.
     */
    @WithTransaction
    @Override
    public Uni<Member> removeClub(UUID id, UUID clubId) {
        return requireClub(clubId)
                .chain(() -> memberAccess.findForClubChange(id, clubId))
                .invoke(member -> {
                    if (member.clubIds().equals(Set.of(clubId))) {
                        throw new LastClubException(id);
                    }
                    member.getClubs().removeIf(club -> club.getId().equals(clubId));
                })
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

    /**
     * A new Keycloak user already has the lowest Role through the realm's default roles.
     */
    private Uni<Void> assignRole(String userId, Role role) {
        return role == Role.MEMBER ? Uni.createFrom().voidItem() : registerService.assignRole(userId, role);
    }

    /**
     * Adds the Member's Role from Keycloak when the current user is an Admin. A Member whose Keycloak user is gone
     * is shown without one.
     */
    private Uni<Member> withRoleForAdmin(Member member) {
        if (!memberAccess.isAdmin()) {
            return Uni.createFrom().item(member);
        }
        return registerService.role(member.id().toString())
                .map(role -> role.map(member::withRole).orElse(member));
    }

    private Uni<ClubEntity> requireClub(UUID id) {
        return findClubs(Set.of(id)).map(clubs -> clubs.iterator().next());
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

    /**
     * Flushes the new Member, so a Member stored meanwhile with the same id or email fails here, inside the caller's
     * chain, as an {@link AlreadyExistsException}, and not at commit.
     */
    private Uni<Member> persist(UUID id, MemberEntity entity, Set<ClubEntity> clubs) {
        entity.setId(id);
        entity.setClubs(clubs);
        return repository.persistAndFlush(entity)
                .onFailure().transform(failure -> MemberConstraints.translateViolationOrPassThrough(failure, entity))
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
