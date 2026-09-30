package dk.wandywharang.service.register;

import dk.wandywharang.api.CreateMemberRequest;
import io.smallrye.mutiny.Uni;

public interface RegisterService {

    /**
     * Creates the member as a Keycloak user. The realm's default roles grant the {@code member} role.
     *
     * @return the Keycloak user id, which is also used as the member id
     */
    Uni<String> register(CreateMemberRequest request);

    /**
     * Asks Keycloak to email the user a link for setting a password and verifying the email address.
     */
    Uni<Void> sendSetupEmail(String userId);

    /**
     * Removes the Keycloak user again, e.g. when persisting the member failed.
     */
    Uni<Void> unregister(String userId);
}
