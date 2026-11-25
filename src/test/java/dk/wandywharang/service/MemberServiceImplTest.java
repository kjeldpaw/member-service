package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Address;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.ClubRepository;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {
    @Mock
    MemberAccess memberAccess;
    @Mock
    MemberMapper mapper;
    @Mock
    MemberRepository repository;
    @Mock
    ClubRepository clubRepository;
    @Mock
    RegisterService registerService;

    MemberServiceImpl service;
    ClubEntity club;
    Member dto;

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(memberAccess, mapper, repository, clubRepository, registerService);
        club = TestData.club();
        dto = TestData.member();
        lenient().when(repository.fetchDetails(any())).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        lenient().when(mapper.map(any(MemberEntity.class))).thenReturn(dto);
    }

    @SuppressWarnings("unchecked")
    private static PanacheQuery<MemberEntity> query(List<MemberEntity> members) {
        final PanacheQuery<MemberEntity> query = mock(PanacheQuery.class);
        when(query.list()).thenReturn(Uni.createFrom().item(members));
        return query;
    }

    private CreateMemberRequest createRequest() {
        return createRequest(Set.of(club.getId()));
    }

    private static CreateMemberRequest createRequest(Set<UUID> clubIds) {
        return CreateMemberRequest.builder()
                .firstName("Kim")
                .lastName("Lee")
                .address(new Address("Main Street 1", "Aarhus", "8000"))
                .phone(Optional.empty())
                .email("kim@example.com")
                .dateOfBirth(Optional.empty())
                .clubIds(clubIds)
                .build();
    }

    @Test
    void adminSeesAllMembers() {
        final var members = List.of(TestData.member(club), TestData.member(TestData.club()));
        final var query = query(members);
        when(memberAccess.isAdmin()).thenReturn(true);
        when(repository.findAll()).thenReturn(query);

        final var result = service.findAll().await().indefinitely();

        assertEquals(2, result.size());
        verify(repository, times(2)).fetchDetails(any());
    }

    @Test
    void othersSeeMembersOfOwnClub() {
        final var profile = TestData.member(club);
        when(memberAccess.isAdmin()).thenReturn(false);
        when(memberAccess.profile()).thenReturn(Uni.createFrom().item(profile));
        when(repository.findSharingClubWith(profile)).thenReturn(Uni.createFrom().item(List.of(profile)));

        final var result = service.findAll().await().indefinitely();

        assertEquals(List.of(dto), result);
    }

    @Test
    void findByIdOnlyReturnsVisibleMembers() {
        final var id = UUID.randomUUID();
        when(memberAccess.findVisible(id)).thenReturn(Uni.createFrom().failure(new NotFoundException()));

        assertThrows(NotFoundException.class, () -> service.findById(id).await().indefinitely());
        verify(mapper, never()).map(any(MemberEntity.class));
    }

    @Test
    void createRegistersInKeycloakPersistsAndSendsSetupEmail() {
        final var userId = UUID.randomUUID();
        final var entity = new MemberEntity();
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId.toString()));
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(mapper.map(request)).thenReturn(entity);
        when(repository.persist(any(MemberEntity.class))).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        when(registerService.sendSetupEmail(userId.toString())).thenReturn(Uni.createFrom().voidItem());

        final var result = service.create(request).await().indefinitely();

        assertSame(dto, result);
        assertEquals(userId, entity.getId());
        assertEquals(Set.of(club), entity.getClubs());
        verify(registerService).sendSetupEmail(userId.toString());
        verify(registerService, never()).unregister(anyString());
    }

    @Test
    void createInForbiddenClubNeverReachesKeycloak() {
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class, () -> service.create(request).await().indefinitely());
        verifyNoInteractions(registerService, clubRepository);
    }

    @Test
    void createInUnknownClubIsNotFoundAndNeverReachesKeycloak() {
        final var unknown = UUID.randomUUID();
        final var request = createRequest(Set.of(club.getId(), unknown));
        when(memberAccess.requireEditableClubs(request.clubIds())).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(request.clubIds())).thenReturn(Uni.createFrom().item(Set.of(club)));

        final var failure = assertThrows(NotFoundException.class, () -> service.create(request).await().indefinitely());
        assertEquals(String.format("Clubs with ids = [%s] not found", unknown), failure.getMessage());
        verifyNoInteractions(registerService);
    }

    @Test
    void createRemovesKeycloakUserWhenPersistingFails() {
        final var userId = UUID.randomUUID().toString();
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        when(mapper.map(request)).thenReturn(new MemberEntity());
        when(repository.persist(any(MemberEntity.class))).thenReturn(Uni.createFrom().failure(new IllegalStateException("Database down")));
        when(registerService.unregister(userId)).thenReturn(Uni.createFrom().voidItem());

        assertThrows(IllegalStateException.class, () -> service.create(request).await().indefinitely());
        verify(registerService).unregister(userId);
        verify(registerService, never()).sendSetupEmail(anyString());
    }

    @Test
    void createFailsWithPersistFailureEvenWhenRemovingKeycloakUserFails() {
        final var userId = UUID.randomUUID().toString();
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        when(mapper.map(request)).thenReturn(new MemberEntity());
        when(repository.persist(any(MemberEntity.class))).thenReturn(Uni.createFrom().failure(new IllegalStateException("Database down")));
        when(registerService.unregister(userId)).thenReturn(Uni.createFrom().failure(new RegisterException(500, "Keycloak down")));

        assertThrows(IllegalStateException.class, () -> service.create(request).await().indefinitely());
    }

    @Test
    void deleteFailsWhenKeycloakUserCannotBeRemoved() {
        final var entity = TestData.member(club);
        when(memberAccess.findDeletable(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(repository.deleteWithDependents(entity)).thenReturn(Uni.createFrom().voidItem());
        when(registerService.unregister(entity.getId().toString())).thenReturn(Uni.createFrom().failure(new RegisterException(500, "Keycloak down")));

        assertThrows(RegisterException.class, () -> service.delete(entity.getId()).await().indefinitely());
    }

    @Test
    void createSucceedsWhenSetupEmailFails() {
        final var userId = UUID.randomUUID().toString();
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(mapper.map(request)).thenReturn(new MemberEntity());
        when(repository.persist(any(MemberEntity.class))).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        when(registerService.sendSetupEmail(userId)).thenReturn(Uni.createFrom().failure(new RuntimeException("No SMTP server")));

        assertSame(dto, service.create(request).await().indefinitely());
        verify(registerService, never()).unregister(anyString());
    }

    @Test
    void updateAppliesRequestToEditableMember() {
        final var entity = TestData.member(club);
        final var request = UpdateMemberRequest.builder().firstName("Sam").build();
        when(memberAccess.findEditable(entity.getId())).thenReturn(Uni.createFrom().item(entity));

        assertSame(dto, service.update(entity.getId(), request).await().indefinitely());
        verify(mapper).map(request, entity);
    }

    @Test
    void updateOfForbiddenMemberChangesNothing() {
        final var id = UUID.randomUUID();
        final var request = UpdateMemberRequest.builder().firstName("Sam").build();
        when(memberAccess.findEditable(id)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class, () -> service.update(id, request).await().indefinitely());
        verify(mapper, never()).map(any(UpdateMemberRequest.class), any(MemberEntity.class));
    }
}
