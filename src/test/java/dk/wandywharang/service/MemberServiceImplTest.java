package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Address;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.Role;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.ClubRepository;
import dk.wandywharang.repository.MemberRepository;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
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
        lenient().when(memberAccess.requireMayGiveRole(any())).thenReturn(Uni.createFrom().voidItem());
    }

    private CreateMemberRequest createRequest() {
        return createRequest(Set.of(club.getId()));
    }

    private static CreateMemberRequest createRequest(Set<UUID> clubIds) {
        return createRequest(clubIds, null);
    }

    private static CreateMemberRequest createRequest(Set<UUID> clubIds, Role role) {
        return CreateMemberRequest.builder()
                .firstName("Kim")
                .lastName("Lee")
                .address(new Address("Main Street 1", "Aarhus", "8000"))
                .phone(Optional.empty())
                .email("kim@example.com")
                .dateOfBirth(Optional.empty())
                .clubIds(clubIds)
                .role(role)
                .build();
    }

    /**
     * Stubs a create of the request that gets as far as storing the Member.
     */
    private String stubCreate(CreateMemberRequest request) {
        final var userId = UUID.randomUUID().toString();
        when(memberAccess.requireEditableClubs(request.clubIds())).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(request.clubIds())).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        lenient().when(mapper.map(request)).thenReturn(new MemberEntity());
        lenient().when(repository.persistAndFlush(any(MemberEntity.class))).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
        lenient().when(registerService.sendSetupEmail(userId)).thenReturn(Uni.createFrom().voidItem());
        return userId;
    }

    @Test
    void findAllReturnsVisibleMembers() {
        final var members = List.of(TestData.member(club), TestData.member(club));
        when(memberAccess.findAllVisible()).thenReturn(Uni.createFrom().item(members));

        final var result = service.findAll().await().indefinitely();

        assertEquals(List.of(dto, dto), result);
        verify(repository, times(2)).fetchDetails(any());
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
        when(repository.persistAndFlush(any(MemberEntity.class))).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
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
        when(repository.persistAndFlush(any(MemberEntity.class))).thenReturn(Uni.createFrom().failure(new IllegalStateException("Database down")));
        when(registerService.unregister(userId)).thenReturn(Uni.createFrom().voidItem());

        assertThrows(IllegalStateException.class, () -> service.create(request).await().indefinitely());
        verify(registerService).unregister(userId);
        verify(registerService, never()).sendSetupEmail(anyString());
    }

    @Test
    void createIsAConflictAndRemovesKeycloakUserWhenTheDatabaseAlreadyHasTheEmail() {
        final var userId = UUID.randomUUID().toString();
        final var request = createRequest();
        final var entity = new MemberEntity();
        entity.setEmail("kim@example.com");
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        when(mapper.map(request)).thenReturn(entity);
        when(repository.persistAndFlush(any(MemberEntity.class))).thenReturn(Uni.createFrom().failure(
                new ConstraintViolationException("duplicate key", new SQLException("duplicate key", "23505"),
                        MemberConstraints.EMAIL)));
        when(registerService.unregister(userId)).thenReturn(Uni.createFrom().voidItem());

        final var failure = assertThrows(AlreadyExistsException.class,
                () -> service.create(request).await().indefinitely());
        assertEquals("A person with email kim@example.com already exists", failure.getMessage());
        verify(registerService).unregister(userId);
    }

    @Test
    void createFailsWithPersistFailureEvenWhenRemovingKeycloakUserFails() {
        final var userId = UUID.randomUUID().toString();
        final var request = createRequest();
        when(memberAccess.requireEditableClubs(Set.of(club.getId()))).thenReturn(Uni.createFrom().voidItem());
        when(clubRepository.findByIds(Set.of(club.getId()))).thenReturn(Uni.createFrom().item(Set.of(club)));
        when(registerService.register(request)).thenReturn(Uni.createFrom().item(userId));
        when(mapper.map(request)).thenReturn(new MemberEntity());
        when(repository.persistAndFlush(any(MemberEntity.class))).thenReturn(Uni.createFrom().failure(new IllegalStateException("Database down")));
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
        when(repository.persistAndFlush(any(MemberEntity.class))).thenAnswer(invocation -> Uni.createFrom().item(invocation.<MemberEntity>getArgument(0)));
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

    @Test
    void createWithoutRoleLeavesTheDefaultRolesAlone() {
        final var request = createRequest();
        stubCreate(request);

        assertEquals(Role.MEMBER, request.role());
        service.create(request).await().indefinitely();

        verify(registerService, never()).assignRole(anyString(), any());
    }

    @Test
    void adminCreatesMemberWithRoleAndGetsTheRoleBack() {
        final var request = createRequest(Set.of(club.getId()), Role.INSTRUCTOR);
        final var userId = stubCreate(request);
        when(registerService.assignRole(userId, Role.INSTRUCTOR)).thenReturn(Uni.createFrom().voidItem());
        when(memberAccess.isAdmin()).thenReturn(true);

        final var result = service.create(request).await().indefinitely();

        assertEquals(Optional.of(Role.INSTRUCTOR), result.role());
        verify(registerService).assignRole(userId, Role.INSTRUCTOR);
    }

    @Test
    void instructorCreatesMemberWithoutGettingTheRoleBack() {
        final var request = createRequest();
        stubCreate(request);

        assertEquals(Optional.empty(), service.create(request).await().indefinitely().role());
    }

    @Test
    void createWithForbiddenRoleNeverReachesKeycloak() {
        final var request = createRequest(Set.of(club.getId()), Role.ADMIN);
        when(memberAccess.requireMayGiveRole(Role.ADMIN)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class, () -> service.create(request).await().indefinitely());
        verifyNoInteractions(registerService, clubRepository);
    }

    @Test
    void createRemovesKeycloakUserWhenAssigningTheRoleFails() {
        final var request = createRequest(Set.of(club.getId()), Role.ADMIN);
        final var userId = stubCreate(request);
        when(registerService.assignRole(userId, Role.ADMIN)).thenReturn(Uni.createFrom().failure(new RegisterException(500, "Keycloak down")));
        when(registerService.unregister(userId)).thenReturn(Uni.createFrom().voidItem());

        assertThrows(RegisterException.class, () -> service.create(request).await().indefinitely());
        verify(registerService).unregister(userId);
        verify(repository, never()).persistAndFlush(any(MemberEntity.class));
        verify(registerService, never()).sendSetupEmail(anyString());
    }

    @Test
    void adminSeesTheRoleOfAMember() {
        final var entity = TestData.member(club);
        when(memberAccess.findVisible(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(memberAccess.isAdmin()).thenReturn(true);
        when(registerService.role(dto.id().toString())).thenReturn(Uni.createFrom().item(Optional.of(Role.ADMIN)));

        assertEquals(Optional.of(Role.ADMIN), service.findById(entity.getId()).await().indefinitely().role());
    }

    @Test
    void adminSeesAMemberWithoutKeycloakUserWithoutRole() {
        final var entity = TestData.member(club);
        when(memberAccess.findVisible(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(memberAccess.isAdmin()).thenReturn(true);
        when(registerService.role(dto.id().toString())).thenReturn(Uni.createFrom().item(Optional.empty()));

        assertEquals(Optional.empty(), service.findById(entity.getId()).await().indefinitely().role());
    }

    @Test
    void instructorDoesNotSeeTheRoleOfAMember() {
        final var entity = TestData.member(club);
        when(memberAccess.findVisible(entity.getId())).thenReturn(Uni.createFrom().item(entity));

        assertEquals(Optional.empty(), service.findById(entity.getId()).await().indefinitely().role());
        verify(registerService, never()).role(anyString());
    }

    @Test
    void updateRoleAssignsItInKeycloakAndReturnsIt() {
        final var entity = TestData.member(club);
        when(memberAccess.findForRoleChange(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(registerService.assignRole(entity.getId().toString(), Role.INSTRUCTOR)).thenReturn(Uni.createFrom().voidItem());

        final var result = service.updateRole(entity.getId(), Role.INSTRUCTOR).await().indefinitely();

        assertEquals(Optional.of(Role.INSTRUCTOR), result.role());
        verify(registerService).assignRole(entity.getId().toString(), Role.INSTRUCTOR);
    }

    @Test
    void forbiddenRoleChangeNeverReachesKeycloak() {
        final var id = UUID.randomUUID();
        when(memberAccess.findForRoleChange(id)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        assertThrows(ForbiddenException.class, () -> service.updateRole(id, Role.ADMIN).await().indefinitely());
        verifyNoInteractions(registerService);
    }
}
