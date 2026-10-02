package dk.wandywharang.service;

import dk.wandywharang.api.CreateGraduationRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateGraduationRequest;
import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.GraduationMapper;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.BeltRepository;
import dk.wandywharang.repository.GraduationRepository;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A member's graduations form a history: the member points to the latest graduation, and each graduation points to
 * the one before it.
 */
@RequestScoped
@RequiredArgsConstructor
public class GraduationServiceImpl implements GraduationService {
    private final MemberAccess memberAccess;
    private final MemberRepository memberRepository;
    private final GraduationRepository repository;
    private final BeltRepository beltRepository;
    private final GraduationMapper mapper;
    private final MemberMapper memberMapper;

    @WithTransaction
    @Override
    public Uni<Member> create(UUID memberId, CreateGraduationRequest request) {
        return memberAccess.findEditable(memberId)
                .call(member -> findBelt(request.beltId())
                        .chain(belt -> findExaminers(request.graduatedBy())
                                .chain(examiners -> {
                                    final var graduation = mapper.map(request);
                                    graduation.setBelt(belt);
                                    graduation.setGraduatedBy(examiners);
                                    graduation.setPreviousGraduation(member.getGraduation());
                                    member.setGraduation(graduation);
                                    return repository.persist(graduation);
                                })))
                .chain(memberRepository::fetchDetails)
                .map(memberMapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> update(UUID memberId, UUID graduationId, UpdateGraduationRequest request) {
        return memberAccess.findEditable(memberId)
                .call(member -> findBelt(request.beltId())
                        .chain(belt -> findExaminers(request.graduatedBy())
                                .invoke(Unchecked.consumer(examiners -> {
                                    final var graduation = findGraduation(member, graduationId);
                                    mapper.map(request, graduation);
                                    graduation.setBelt(belt);
                                    graduation.setGraduatedBy(examiners);
                                }))))
                .chain(memberRepository::fetchDetails)
                .map(memberMapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> delete(UUID memberId, UUID graduationId) {
        return memberAccess.findEditable(memberId)
                .call(member -> {
                    final var graduation = findGraduation(member, graduationId);
                    final var previous = graduation.getPreviousGraduation().orElse(null);
                    final var next = findNext(member, graduation);
                    // Link the neighbours past the deleted graduation
                    if (next == null) {
                        member.setGraduation(previous);
                    } else {
                        next.setPreviousGraduation(previous);
                    }
                    return repository.delete(graduation);
                })
                .chain(memberRepository::fetchDetails)
                .map(memberMapper::map);
    }

    private Uni<BeltEntity> findBelt(UUID beltId) {
        return beltRepository.findById(beltId)
                .onItem().ifNull().failWith(() -> new NotFoundException(String.format("Belt with id = %s not found", beltId)));
    }

    private Uni<Set<MemberEntity>> findExaminers(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Uni.createFrom().item(new HashSet<>());
        }
        return memberRepository.list("id in ?1", ids)
                .map(Unchecked.function(examiners -> {
                    if (examiners.size() != ids.size()) {
                        final var unknown = new HashSet<>(ids);
                        examiners.forEach(examiner -> unknown.remove(examiner.getId()));
                        throw new BadRequestException(String.format("Examiners with ids = %s not found", unknown));
                    }
                    return new HashSet<>(examiners);
                }));
    }

    private static GraduationEntity findGraduation(MemberEntity member, UUID graduationId) {
        for (var graduation = member.getGraduation(); graduation != null;
             graduation = graduation.getPreviousGraduation().orElse(null)) {
            if (graduation.getId().equals(graduationId)) {
                return graduation;
            }
        }
        throw new NotFoundException(
                String.format("Graduation with id = %s not found for member with id = %s", graduationId, member.getId()));
    }

    /**
     * @return the graduation that comes after the given one, or {@code null} if it is the member's latest graduation
     */
    private static GraduationEntity findNext(MemberEntity member, GraduationEntity graduation) {
        GraduationEntity next = null;
        for (var current = member.getGraduation(); current != graduation;
             current = current.getPreviousGraduation().orElseThrow()) {
            next = current;
        }
        return next;
    }
}
