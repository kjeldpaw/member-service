package dk.wandywharang.service;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Role;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {
    private static final String REALM = "wandywharang";

    @Mock
    Keycloak keycloak;
    @Mock
    RealmResource realm;
    @Mock
    UsersResource users;

    RegisterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegisterServiceImpl(keycloak, REALM);
        when(keycloak.realm(REALM)).thenReturn(realm);
        when(realm.users()).thenReturn(users);
    }

    private static CreateMemberRequest request() {
        return CreateMemberRequest.builder()
                .firstName("Kim")
                .lastName("Lee")
                .address(new Address("Main Street 1", "Aarhus", "8000"))
                .phone(Optional.empty())
                .email("kim@example.com")
                .dateOfBirth(Optional.empty())
                .clubIds(Set.of(UUID.randomUUID()))
                .build();
    }

    private static Response response(Response.Status status) {
        final var response = mock(Response.class);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        return response;
    }

    @Test
    void registerCreatesEnabledUserWithoutPasswordAndReturnsItsId() {
        final var userId = UUID.randomUUID().toString();
        final var response = response(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/admin/realms/wandywharang/users/" + userId));
        when(users.create(any())).thenReturn(response);

        assertEquals(userId, service.register(request()).await().indefinitely());

        final var user = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(user.capture());
        assertEquals("kim@example.com", user.getValue().getUsername());
        assertEquals("kim@example.com", user.getValue().getEmail());
        assertEquals("Kim", user.getValue().getFirstName());
        assertEquals("Lee", user.getValue().getLastName());
        assertTrue(user.getValue().isEnabled());
        assertFalse(user.getValue().isEmailVerified());
        assertNull(user.getValue().getCredentials());
        verify(response).close();
    }

    @Test
    void registerOfExistingUserFailsWithConflict() {
        final var response = response(Response.Status.CONFLICT);
        when(users.create(any())).thenReturn(response);

        assertThrows(AlreadyExistsException.class, () -> service.register(request()).await().indefinitely());
    }

    @Test
    void registerFailsWithKeycloakStatus() {
        final var response = response(Response.Status.FORBIDDEN);
        when(response.getStatusInfo()).thenReturn(Response.Status.FORBIDDEN);
        when(users.create(any())).thenReturn(response);

        final var failure = assertThrows(RegisterException.class, () -> service.register(request()).await().indefinitely());
        assertEquals(403, failure.keycloakStatus());
    }

    @Test
    void registerFailsWithKeycloakStatusWhenTheAdminClientThrows() {
        when(users.create(any())).thenThrow(new NotAuthorizedException("Bearer"));

        final var failure = assertThrows(RegisterException.class, () -> service.register(request()).await().indefinitely());
        assertEquals(401, failure.keycloakStatus());
    }

    @Test
    void assignRoleFailsWithKeycloakStatusWhenTheAdminClientThrows() {
        final var userId = UUID.randomUUID().toString();
        when(realmLevelOf(userId).listEffective()).thenThrow(new WebApplicationException(403));

        final var failure = assertThrows(RegisterException.class,
                () -> service.assignRole(userId, Role.INSTRUCTOR).await().indefinitely());
        assertEquals(403, failure.keycloakStatus());
    }

    @Test
    void unreachableKeycloakFailsWithoutStatus() {
        when(users.create(any())).thenThrow(new ProcessingException("Connection refused"));

        final var failure = assertThrows(RegisterException.class, () -> service.register(request()).await().indefinitely());
        assertEquals(0, failure.keycloakStatus());
    }

    @Test
    void sendSetupEmailAsksForPasswordAndEmailVerification() {
        final var userId = UUID.randomUUID().toString();
        final var user = mock(UserResource.class);
        when(users.get(userId)).thenReturn(user);

        service.sendSetupEmail(userId).await().indefinitely();

        verify(user).executeActionsEmail(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));
    }

    @Test
    void unregisterDeletesUser() {
        final var userId = UUID.randomUUID().toString();
        final var response = response(Response.Status.NO_CONTENT);
        when(users.delete(userId)).thenReturn(response);

        service.unregister(userId).await().indefinitely();

        verify(users).delete(userId);
        verify(response).close();
    }

    @Test
    void unregisterOfUnknownUserSucceeds() {
        final var userId = UUID.randomUUID().toString();
        final var response = response(Response.Status.NOT_FOUND);
        when(users.delete(userId)).thenReturn(response);

        service.unregister(userId).await().indefinitely();

        verify(response).close();
    }

    @Test
    void unregisterFailsWithKeycloakStatus() {
        final var userId = UUID.randomUUID().toString();
        final var response = response(Response.Status.INTERNAL_SERVER_ERROR);
        when(response.getStatusInfo()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR);
        when(users.delete(userId)).thenReturn(response);

        final var failure = assertThrows(RegisterException.class, () -> service.unregister(userId).await().indefinitely());
        assertEquals(500, failure.keycloakStatus());
        verify(response).close();
    }

    private static RoleRepresentation role(String name) {
        final var role = new RoleRepresentation();
        role.setId(UUID.randomUUID().toString());
        role.setName(name);
        return role;
    }

    private static List<RoleRepresentation> roles(String... names) {
        return Arrays.stream(names).map(RegisterServiceImplTest::role).toList();
    }

    private RoleScopeResource realmLevelOf(String userId) {
        final var user = mock(UserResource.class);
        final var mappings = mock(RoleMappingResource.class);
        final var realmLevel = mock(RoleScopeResource.class);
        when(users.get(userId)).thenReturn(user);
        when(user.roles()).thenReturn(mappings);
        when(mappings.realmLevel()).thenReturn(realmLevel);
        return realmLevel;
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(ArgumentCaptor<List> roles) {
        return ((List<RoleRepresentation>) roles.getValue()).stream().map(RoleRepresentation::getName).sorted().toList();
    }

    @Test
    void roleIsTheHighestEffectiveRealmRole() {
        final var userId = UUID.randomUUID().toString();
        when(realmLevelOf(userId).listEffective()).thenReturn(roles("default-roles-wandywharang", "member", "admin", "offline_access"));

        assertEquals(Optional.of(Role.ADMIN), service.role(userId).await().indefinitely());
    }

    @Test
    void roleOfUserWithOnlyDefaultRolesIsMember() {
        final var userId = UUID.randomUUID().toString();
        when(realmLevelOf(userId).listEffective()).thenReturn(roles("default-roles-wandywharang", "member"));

        assertEquals(Optional.of(Role.MEMBER), service.role(userId).await().indefinitely());
    }

    @Test
    void roleOfUnknownUserIsEmpty() {
        final var userId = UUID.randomUUID().toString();
        when(realmLevelOf(userId).listEffective()).thenThrow(new WebApplicationException(404));

        assertEquals(Optional.empty(), service.role(userId).await().indefinitely());
    }

    @Test
    void roleFailsWhenKeycloakFailsOtherwise() {
        final var userId = UUID.randomUUID().toString();
        when(realmLevelOf(userId).listEffective()).thenThrow(new WebApplicationException(503));

        final var failure = assertThrows(RegisterException.class, () -> service.role(userId).await().indefinitely());
        assertEquals(503, failure.keycloakStatus());
    }

    @Test
    void assigningAdminAddsInstructorAndAdmin() {
        final var userId = UUID.randomUUID().toString();
        final var realmLevel = realmLevelOf(userId);
        when(realmLevel.listEffective()).thenReturn(roles("default-roles-wandywharang", "member"));
        when(realmLevel.listAvailable()).thenReturn(roles("instructor", "admin", "offline_access"));
        when(realmLevel.listAll()).thenReturn(roles("default-roles-wandywharang"));

        service.assignRole(userId, Role.ADMIN).await().indefinitely();

        final var added = ArgumentCaptor.forClass(List.class);
        verify(realmLevel).add(added.capture());
        assertEquals(List.of("admin", "instructor"), names(added));
        verify(realmLevel, never()).remove(anyList());
    }

    @Test
    void assigningInstructorToAnAdminRemovesOnlyAdmin() {
        final var userId = UUID.randomUUID().toString();
        final var realmLevel = realmLevelOf(userId);
        when(realmLevel.listEffective()).thenReturn(roles("default-roles-wandywharang", "member", "instructor", "admin"));
        when(realmLevel.listAvailable()).thenReturn(roles("offline_access"));
        when(realmLevel.listAll()).thenReturn(roles("default-roles-wandywharang", "instructor", "admin"));

        service.assignRole(userId, Role.INSTRUCTOR).await().indefinitely();

        final var removed = ArgumentCaptor.forClass(List.class);
        verify(realmLevel).remove(removed.capture());
        assertEquals(List.of("admin"), names(removed));
        verify(realmLevel, never()).add(anyList());
    }

    @Test
    void assigningMemberRemovesInstructorAndAdminButNeverMember() {
        final var userId = UUID.randomUUID().toString();
        final var realmLevel = realmLevelOf(userId);
        when(realmLevel.listEffective()).thenReturn(roles("member", "instructor", "admin"));
        when(realmLevel.listAvailable()).thenReturn(roles());
        when(realmLevel.listAll()).thenReturn(roles("member", "instructor", "admin"));

        service.assignRole(userId, Role.MEMBER).await().indefinitely();

        final var removed = ArgumentCaptor.forClass(List.class);
        verify(realmLevel).remove(removed.capture());
        assertEquals(List.of("admin", "instructor"), names(removed));
        verify(realmLevel, never()).add(anyList());
    }

    @Test
    void assigningTheRoleAUserAlreadyHasChangesNothing() {
        final var userId = UUID.randomUUID().toString();
        final var realmLevel = realmLevelOf(userId);
        when(realmLevel.listEffective()).thenReturn(roles("default-roles-wandywharang", "member", "instructor"));
        when(realmLevel.listAvailable()).thenReturn(roles("admin"));
        when(realmLevel.listAll()).thenReturn(roles("default-roles-wandywharang", "instructor"));

        service.assignRole(userId, Role.INSTRUCTOR).await().indefinitely();

        verify(realmLevel, never()).add(anyList());
        verify(realmLevel, never()).remove(anyList());
    }
}
