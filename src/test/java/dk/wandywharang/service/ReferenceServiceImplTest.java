package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.CreateReferenceRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.ReferenceType;
import dk.wandywharang.api.UpdateReferenceRequest;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.entity.ReferenceEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.mapper.ReferenceMapper;
import dk.wandywharang.repository.MemberRepository;
import dk.wandywharang.repository.ReferenceRepository;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceServiceImplTest {
    @Mock
    MemberAccess memberAccess;
    @Mock
    MemberRepository memberRepository;
    @Mock
    ReferenceRepository repository;
    @Mock
    ReferenceMapper mapper;
    @Mock
    MemberMapper memberMapper;

    ReferenceServiceImpl service;
    MemberEntity member;
    Member dto;

    @BeforeEach
    void setUp() {
        service = new ReferenceServiceImpl(memberAccess, memberRepository, repository, mapper, memberMapper);
        member = TestData.member(TestData.club());
        dto = TestData.member();
        lenient().when(memberAccess.findEditable(member.getId())).thenReturn(Uni.createFrom().item(member));
        lenient().when(memberRepository.fetchDetails(any())).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        lenient().when(memberMapper.map(member)).thenReturn(dto);
    }

    @Test
    void createAddsReferenceToMember() {
        final var request = new CreateReferenceRequest(ReferenceType.KukkiWon, "KW-123");
        final var reference = new ReferenceEntity();
        when(mapper.map(request)).thenReturn(reference);
        when(repository.persist(reference)).thenReturn(Uni.createFrom().item(reference));

        assertSame(dto, service.create(member.getId(), request).await().indefinitely());
        assertSame(member, reference.getMember());
        assertTrue(member.getReferences().contains(reference));
    }

    @Test
    void createForForbiddenMemberPersistsNothing() {
        final var id = UUID.randomUUID();
        when(memberAccess.findEditable(id)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class,
                () -> service.create(id, new CreateReferenceRequest(ReferenceType.KukkiWon, "KW-123")).await().indefinitely());
        verify(repository, never()).persist(any(ReferenceEntity.class));
    }

    @Test
    void updateChangesReferenceOfMember() {
        final var reference = TestData.withId(new ReferenceEntity());
        final var request = new UpdateReferenceRequest("KW-456");
        when(repository.findByMember(member.getId(), reference.getId())).thenReturn(Uni.createFrom().item(reference));

        assertSame(dto, service.update(member.getId(), reference.getId(), request).await().indefinitely());
        verify(mapper).map(request, reference);
    }

    @Test
    void updateOfUnknownReferenceIsNotFound() {
        final var referenceId = UUID.randomUUID();
        when(repository.findByMember(member.getId(), referenceId)).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class,
                () -> service.update(member.getId(), referenceId, new UpdateReferenceRequest("KW-456")).await().indefinitely());
    }

    @Test
    void deleteRemovesReferenceFromMember() {
        final var reference = TestData.withId(new ReferenceEntity());
        member.getReferences().add(reference);
        when(repository.findByMember(member.getId(), reference.getId())).thenReturn(Uni.createFrom().item(reference));
        when(repository.delete(reference)).thenReturn(Uni.createFrom().voidItem());

        assertSame(dto, service.delete(member.getId(), reference.getId()).await().indefinitely());
        assertFalse(member.getReferences().contains(reference));
        verify(repository).delete(reference);
    }

    @Test
    void deleteOfUnknownReferenceIsNotFound() {
        final var referenceId = UUID.randomUUID();
        when(repository.findByMember(member.getId(), referenceId)).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> service.delete(member.getId(), referenceId).await().indefinitely());
        verify(repository, never()).delete(any(ReferenceEntity.class));
    }
}
