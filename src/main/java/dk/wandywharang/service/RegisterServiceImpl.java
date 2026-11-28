package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Role;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JBossLog
@ApplicationScoped
public class RegisterServiceImpl implements RegisterService {
    private static final List<String> SETUP_ACTIONS = List.of("UPDATE_PASSWORD", "VERIFY_EMAIL");
    /**
     * The realm roles that the Role decides. {@code member} is left alone: every user has it.
     */
    private static final Set<String> ROLE_REALM_ROLES = Stream.of(Role.INSTRUCTOR, Role.ADMIN)
            .map(Role::realmRole)
            .collect(Collectors.toUnmodifiableSet());

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
                    throw new AlreadyExistsException(request.email());
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
    public Uni<Optional<Role>> role(String userId) {
        return blocking(() -> {
            try {
                final var effective = users().get(userId).roles().realmLevel().listEffective();
                return Optional.of(Role.highestOf(names(effective)));
            } catch (WebApplicationException e) {
                // The admin client reports an unknown user as a 404, not always as a NotFoundException
                if (e.getResponse().getStatus() == 404) {
                    return Optional.empty();
                }
                throw e;
            }
        });
    }

    /**
     * Roles are added only when the user doesn't already hold them, directly or through a composite, and removed only
     * when they are mapped directly. The roles to add come from the user's available realm roles, so the service
     * account needs no right to view the realm's roles.
     */
    @Override
    public Uni<Void> assignRole(String userId, Role role) {
        return blocking(() -> {
            final var realmLevel = users().get(userId).roles().realmLevel();
            final var wanted = role.realmRoles();
            final var held = names(realmLevel.listEffective());
            final var toAdd = realmLevel.listAvailable().stream()
                    .filter(available -> wanted.contains(available.getName()) && !held.contains(available.getName()))
                    .toList();
            final var toRemove = realmLevel.listAll().stream()
                    .filter(direct -> ROLE_REALM_ROLES.contains(direct.getName()) && !wanted.contains(direct.getName()))
                    .toList();
            if (!toAdd.isEmpty()) {
                realmLevel.add(toAdd);
            }
            if (!toRemove.isEmpty()) {
                realmLevel.remove(toRemove);
            }
            return null;
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
                    throw new RegisterException(response.getStatus(), response.getStatusInfo().getReasonPhrase());
                }
            }
            return null;
        });
    }

    private static Set<String> names(Collection<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
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
     * Vert.x context, which Hibernate Reactive requires for any work chained after it. A failure of the admin client
     * itself becomes a {@link RegisterException}, so Keycloak's own status never reaches the caller as theirs.
     */
    private static <T> Uni<T> blocking(Supplier<T> supplier) {
        return Uni.createFrom().deferred(() -> {
            final Context context = Vertx.currentContext();
            final var uni = Uni.createFrom().item(supplier)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .onFailure(WebApplicationException.class).transform(e -> new RegisterException(
                            ((WebApplicationException) e).getResponse().getStatus(), e.getMessage(), e))
                    .onFailure(ProcessingException.class).transform(e -> new RegisterException(0, e.getMessage(), e));
            return context == null ? uni : uni.emitOn(command -> context.runOnContext(ignored -> command.run()));
        });
    }
}
