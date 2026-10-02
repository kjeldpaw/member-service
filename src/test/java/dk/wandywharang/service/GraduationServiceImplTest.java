package dk.wandywharang.service;

import dk.wandywharang.TestData;
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
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraduationServiceImplTest {
    @Mock
    MemberAccess memberAccess;
    @Mock
    MemberRepository memberRepository;
    @Mock
    GraduationRepository repository;
    @Mock
    BeltRepository beltRepository;
    @Mock
    GraduationMapper mapper;
    @Mock
    MemberMapper memberMapper;

    GraduationServiceImpl service;
    MemberEntity member;
    MemberEntity examiner;
    BeltEntity belt;
    Member dto;

    @BeforeEach
    void setUp() {
        service = new GraduationServiceImpl(memberAccess, memberRepository, repository, beltRepository, mapper, memberMapper);
        member = TestData.member(TestData.club());
        examiner = TestData.member(member.getClub());
        belt = TestData.belt();
        dto = TestData.member();
        lenient().when(memberAccess.findEditable(member.getId())).thenReturn(Uni.createFrom().item(member));
        lenient().when(memberRepository.fetchDetails(any())).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        lenient().when(memberMapper.map(member)).thenReturn(dto);
        lenient().when(beltRepository.findById(any(UUID.class))).thenReturn(Uni.createFrom().nullItem());
        lenient().when(beltRepository.findById(belt.getId())).thenReturn(Uni.createFrom().item(belt));
        lenient().when(memberRepository.list("id in ?1", Set.of(examiner.getId())))
                .thenReturn(Uni.createFrom().item(List.of(examiner)));
    }

    private CreateGraduationRequest createRequest(UUID beltId, Set<UUID> examiners) {
        return new CreateGraduationRequest(LocalDate.of(2026, 6, 1), examiners, beltId);
    }

    private UpdateGraduationRequest updateRequest() {
        return new UpdateGraduationRequest(LocalDate.of(2026, 7, 1), Set.of(examiner.getId()), belt.getId());
    }

    @Test
    void createMakesNewGraduationTheLatest() {
        final var first = TestData.graduation(null);
        member.setGraduation(first);
        final var request = createRequest(belt.getId(), Set.of(examiner.getId()));
        final var graduation = new GraduationEntity();
        when(mapper.map(request)).thenReturn(graduation);
        when(repository.persist(graduation)).thenReturn(Uni.createFrom().item(graduation));

        assertSame(dto, service.create(member.getId(), request).await().indefinitely());
        assertSame(graduation, member.getGraduation());
        assertSame(first, graduation.getPreviousGraduation().orElseThrow());
        assertSame(belt, graduation.getBelt());
        assertEquals(Set.of(examiner), graduation.getGraduatedBy());
    }

    @Test
    void createWithoutExaminersSkipsLookup() {
        final var request = createRequest(belt.getId(), Set.of());
        final var graduation = new GraduationEntity();
        when(mapper.map(request)).thenReturn(graduation);
        when(repository.persist(graduation)).thenReturn(Uni.createFrom().item(graduation));

        service.create(member.getId(), request).await().indefinitely();

        assertEquals(Set.of(), graduation.getGraduatedBy());
        verify(memberRepository, never()).list("id in ?1", Set.of());
    }

    @Test
    void createWithUnknownBeltIsNotFound() {
        final var request = createRequest(UUID.randomUUID(), Set.of(examiner.getId()));

        assertThrows(NotFoundException.class, () -> service.create(member.getId(), request).await().indefinitely());
        verify(repository, never()).persist(any(GraduationEntity.class));
    }

    @Test
    void createWithUnknownExaminerIsBadRequest() {
        final var unknown = UUID.randomUUID();
        final var request = createRequest(belt.getId(), Set.of(unknown));
        when(memberRepository.list("id in ?1", Set.of(unknown))).thenReturn(Uni.createFrom().item(List.of()));

        final var failure = assertThrows(BadRequestException.class,
                () -> service.create(member.getId(), request).await().indefinitely());
        assertEquals(String.format("Examiners with ids = [%s] not found", unknown), failure.getMessage());
        verify(repository, never()).persist(any(GraduationEntity.class));
    }

    @Test
    void createForForbiddenMemberPersistsNothing() {
        final var id = UUID.randomUUID();
        when(memberAccess.findEditable(id)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class,
                () -> service.create(id, createRequest(belt.getId(), Set.of())).await().indefinitely());
        verify(repository, never()).persist(any(GraduationEntity.class));
    }

    @Test
    void updateChangesGraduationInHistory() {
        final var first = TestData.graduation(null);
        member.setGraduation(TestData.graduation(first));
        final var request = updateRequest();

        assertSame(dto, service.update(member.getId(), first.getId(), request).await().indefinitely());
        verify(mapper).map(request, first);
        assertSame(belt, first.getBelt());
        assertEquals(Set.of(examiner), first.getGraduatedBy());
    }

    @Test
    void updateOfGraduationOutsideHistoryIsNotFound() {
        member.setGraduation(TestData.graduation(null));

        assertThrows(NotFoundException.class,
                () -> service.update(member.getId(), UUID.randomUUID(), updateRequest()).await().indefinitely());
    }

    @Test
    void deleteOfLatestMakesPreviousTheLatest() {
        final var first = TestData.graduation(null);
        final var second = TestData.graduation(first);
        member.setGraduation(second);
        when(repository.delete(second)).thenReturn(Uni.createFrom().voidItem());

        assertSame(dto, service.delete(member.getId(), second.getId()).await().indefinitely());
        assertSame(first, member.getGraduation());
        verify(repository).delete(second);
    }

    @Test
    void deleteInMiddleOfHistoryLinksNeighbours() {
        final var first = TestData.graduation(null);
        final var second = TestData.graduation(first);
        final var third = TestData.graduation(second);
        member.setGraduation(third);
        when(repository.delete(second)).thenReturn(Uni.createFrom().voidItem());

        service.delete(member.getId(), second.getId()).await().indefinitely();

        assertSame(third, member.getGraduation());
        assertSame(first, third.getPreviousGraduation().orElseThrow());
    }

    @Test
    void deleteOfOnlyGraduationLeavesMemberWithout() {
        final var only = TestData.graduation(null);
        member.setGraduation(only);
        when(repository.delete(only)).thenReturn(Uni.createFrom().voidItem());

        service.delete(member.getId(), only.getId()).await().indefinitely();

        assertNull(member.getGraduation());
    }

    @Test
    void deleteOfGraduationOutsideHistoryIsNotFound() {
        member.setGraduation(TestData.graduation(null));

        assertThrows(NotFoundException.class,
                () -> service.delete(member.getId(), UUID.randomUUID()).await().indefinitely());
        verify(repository, never()).delete(any(GraduationEntity.class));
    }
}
