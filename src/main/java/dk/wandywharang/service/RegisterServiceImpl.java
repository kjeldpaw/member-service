package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.function.Supplier;

@JBossLog
@ApplicationScoped
public class RegisterServiceImpl implements RegisterService {
    private static final List<String> SETUP_ACTIONS = List.of("UPDATE_PASSWORD", "VERIFY_EMAIL");

    private final Keycloak keycloak;
    private final String realm;

    public RegisterServiceImpl(Keycloak keycloak,
                               @ConfigProperty(name = "member-service.keycloak.realm") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public Uni<String> register(CreateMemberRequest request) {
        return blocking(() -> {
            try (final var response = users().create(build(request))) {
                if (response.getStatus() == 409) {
                    throw new AlreadyExistsException();
                }
                if (response.getStatus() != 201) {
                    throw new RegisterException(response.getStatus(), response.getStatusInfo().getReasonPhrase());
                }

                // Extract the new user's ID from the Location header
                final var location = response.getLocation().getPath();
                return location.substring(location.lastIndexOf('/') + 1);
            }
        });
    }

    @Override
    public Uni<Void> sendSetupEmail(String userId) {
        return blocking(() -> {
            users().get(userId).executeActionsEmail(SETUP_ACTIONS);
            return null;
        });
    }

    @Override
    public Uni<Void> unregister(String userId) {
        return blocking(() -> {
            try (final var response = users().delete(userId)) {
                if (response.getStatus() != 204 && response.getStatus() != 404) {
                    log.warnf("Could not delete Keycloak user %s: %d", userId, response.getStatus());
                }
            }
            return null;
        });
    }

    private UsersResource users() {
        return keycloak.realm(realm).users();
    }

    private UserRepresentation build(CreateMemberRequest request) {
        final var user = new UserRepresentation();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        return user;
    }

    /**
     * The Keycloak admin client is blocking, so run it on a worker thread and emit the result back on the caller's
     * Vert.x context, which Hibernate Reactive requires for any work chained after it.
     */
    private static <T> Uni<T> blocking(Supplier<T> supplier) {
        return Uni.createFrom().deferred(() -> {
            final Context context = Vertx.currentContext();
            final var uni = Uni.createFrom().item(supplier)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
            return context == null ? uni : uni.emitOn(command -> context.runOnContext(ignored -> command.run()));
        });
    }
}
