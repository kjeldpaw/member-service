package dk.wandywharang.service;

import dk.wandywharang.api.Register;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class RegisterServiceImpl implements RegisterService {
    private final Keycloak keycloak;
    private final String realm;

    public RegisterServiceImpl(Keycloak keycloak) {
        this.keycloak = keycloak;
        this.realm = "wandywharang";
    }

    @Override
    public Uni<Void> register(Register register) {
        ExecutorService executor = Executors.newSingleThreadExecutor(WorkerThread::new);
        return Uni.createFrom().item(Unchecked.supplier(() -> build(register)))
                .chain(this::register)
                .runSubscriptionOn(executor);
    }

    private Uni<Void> register(UserRepresentation user) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
                    try (final var response = keycloak.realm(realm).users().create(user)) {
                        // Extract the new user's ID from the Location header
                        String location = response.getLocation().getPath();
                        return location.substring(location.lastIndexOf('/') + 1);
                    }
                }))
                .chain(this::addMemberRole);
    }

    private Uni<Void> addMemberRole(String userId) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            final var roles = keycloak.realm(realm).roles().list();
            final var role = roles.stream()
                    .filter(r -> "member".equalsIgnoreCase(r.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Role 'member' not found in realm " + realm));
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(List.of(role));
            return null;
        }));
    }

    private UserRepresentation build(Register register) {
        final var credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(register.password());
        credential.setTemporary(false);

        final var user = new UserRepresentation();
        user.setUsername(register.email());
        user.setEmail(register.email());
        user.setFirstName(register.firstName());
        user.setLastName(register.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setCredentials(List.of(credential));
        return user;
    }
}
