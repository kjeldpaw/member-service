package dk.wandywharang.service;

import dk.wandywharang.api.Club;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.MemberEntity;
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
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@JBossLog
@RequestScoped
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final SecurityIdentity securityIdentity;
    private final MemberMapper mapper;
    private final MemberRepository repository;
    private final ClubRepository clubRepository;
    private final RegisterService registerService;

    @WithSession
    @Override
    public Uni<List<Member>> findAll() {
        if (securityIdentity.hasRole("admin")) {
            return repository.findAll().list().onItem().transform(entities -> entities.stream().map(mapper::map).toList());
        } else {
            return profile().onItem().transformToUni(profile -> findByClub(profile.getClub()))
                    .map(entities -> entities.stream().map(mapper::map).toList());
        }
    }

    @WithSession
    @Override
    public Uni<Member> findById(UUID id) {
        if (securityIdentity.hasRole("admin")) {
            return repository.findById(id).onItem().ifNotNull().transform(mapper::map);
        } else {
            return Uni.combine().all().unis(profile(), repository.findById(id))
                    .asTuple().onItem().transform(Unchecked.function(tuple -> {
                        if (tuple.getItem1().getClub().getId().equals(tuple.getItem2().getClub().getId())) {
                            return tuple.getItem2();
                        } else {
                            throw new NotFoundException("Member not found");
                        }
                    }))
                    .map(mapper::map);
        }
    }


    /**
     * Creates the Keycloak user first, so its id can be used as the member id. If persisting the member fails, the
     * Keycloak user is removed again. The setup email is only sent once the member is stored.
     */
    @WithTransaction
    @WithSession
    @Override
    public Uni<Member> create(CreateMemberRequest request) {
        return registerService.register(request)
                .chain(userId -> persist(UUID.fromString(userId), request)
                        .onFailure().call(() -> registerService.unregister(userId))
                        .call(() -> registerService.sendSetupEmail(userId)
                                .onFailure().invoke(e -> log.warnf(e, "Could not send setup email to Keycloak user %s", userId))
                                .onFailure().recoverWithNull()));
    }

    @WithTransaction
    @WithSession
    @Override
    public Uni<Member> update(UUID id, UpdateMemberRequest request) {
        if (securityIdentity.hasRole("admin")) {
            return repository.findById(id)
                    .onItem().ifNull().failWith(() -> new NotFoundException(String.format("Member with id = %s not found", id)))
                    .map(member -> mapper.map(request, member))
                    .map(mapper::map);
        } else {
            return Uni.combine().all().unis(profile(), repository.findById(id))
                    .asTuple().onItem().transform(Unchecked.function(tuple -> {
                        if (tuple.getItem1().getClub().getId().equals(tuple.getItem2().getClub().getId())) {
                            return tuple.getItem2();
                        } else {
                            throw new NotAllowedException("Member not belongs to club");
                        }
                    }))
                    .map(mapper::map);
        }
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

    private Uni<MemberEntity> profile() {
        return repository.findById(UUID.fromString(securityIdentity.getPrincipal().getName()))
                .onItem().ifNull().failWith(new NotFoundException(String.format("Profile with id = %s not found", securityIdentity.getPrincipal().getName())));
    }

    private Uni<List<MemberEntity>> findByClub(ClubEntity club) {
        return repository.find("club.id", club.getId()).list();
    }
}
