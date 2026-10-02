package dk.wandywharang.service;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.CreateMemberRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
                .clubId(UUID.randomUUID())
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
        assertEquals(403, failure.getResponse().getStatus());
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
    void unregisterOfFailedDeleteDoesNotFail() {
        final var userId = UUID.randomUUID().toString();
        final var response = response(Response.Status.INTERNAL_SERVER_ERROR);
        when(users.delete(userId)).thenReturn(response);

        service.unregister(userId).await().indefinitely();

        verify(users).delete(userId);
    }
}
