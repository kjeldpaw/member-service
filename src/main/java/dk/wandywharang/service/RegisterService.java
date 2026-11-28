package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Role;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface RegisterService {

    /**
     * Creates the member as a Keycloak user. The realm's default roles grant the {@code member} role.
     *
     * @return the Keycloak user id, which is also used as the member id
     */
    Uni<String> register(CreateMemberRequest request);

    /**
     * The user's Role, from their effective realm roles, or empty if Keycloak doesn't know the user.
     */
    Uni<Optional<Role>> role(String userId);

    /**
     * Gives the user the realm roles of the Role and takes away the {@code instructor} and {@code admin} roles that
     * it doesn't include. The {@code member} role is never taken away.
     */
    Uni<Void> assignRole(String userId, Role role);

    /**
     * Asks Keycloak to email the user a link for setting a password and verifying the email address.
     */
    Uni<Void> sendSetupEmail(String userId);

    /**
     * Removes the Keycloak user. A user Keycloak does not know counts as removed; any other failure fails the
     * returned Uni.
     */
    Uni<Void> unregister(String userId);
}
