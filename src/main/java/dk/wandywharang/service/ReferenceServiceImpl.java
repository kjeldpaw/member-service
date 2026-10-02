package dk.wandywharang.service;

import dk.wandywharang.api.CreateReferenceRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateReferenceRequest;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.entity.ReferenceEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.mapper.ReferenceMapper;
import dk.wandywharang.repository.MemberRepository;
import dk.wandywharang.repository.ReferenceRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.UUID;

@RequestScoped
@RequiredArgsConstructor
public class ReferenceServiceImpl implements ReferenceService {
    private final MemberAccess memberAccess;
    private final MemberRepository memberRepository;
    private final ReferenceRepository repository;
    private final ReferenceMapper mapper;
    private final MemberMapper memberMapper;

    @WithTransaction
    @Override
    public Uni<Member> create(UUID memberId, CreateReferenceRequest request) {
        return memberAccess.findEditable(memberId)
                .chain(memberRepository::fetchDetails)
                .chain(member -> {
                    final var reference = mapper.map(request);
                    reference.setMember(member);
                    if (member.getReferences() == null) {
                        member.setReferences(new HashSet<>());
                    }
                    member.getReferences().add(reference);
                    return repository.persist(reference).replaceWith(member);
                })
                .map(memberMapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> update(UUID memberId, UUID referenceId, UpdateReferenceRequest request) {
        return memberAccess.findEditable(memberId)
                .call(member -> findReference(memberId, referenceId)
                        .invoke(reference -> mapper.map(request, reference)))
                .chain(memberRepository::fetchDetails)
                .map(memberMapper::map);
    }

    @WithTransaction
    @Override
    public Uni<Member> delete(UUID memberId, UUID referenceId) {
        return memberAccess.findEditable(memberId)
                .chain(memberRepository::fetchDetails)
                .call(member -> findReference(memberId, referenceId)
                        .call(reference -> remove(member, reference)))
                .map(memberMapper::map);
    }

    private Uni<ReferenceEntity> findReference(UUID memberId, UUID referenceId) {
        return repository.findByMember(memberId, referenceId)
                .onItem().ifNull().failWith(() -> new NotFoundException(
                        String.format("Reference with id = %s not found for member with id = %s", referenceId, memberId)));
    }

    private Uni<Void> remove(MemberEntity member, ReferenceEntity reference) {
        member.getReferences().remove(reference);
        return repository.delete(reference);
    }
}
