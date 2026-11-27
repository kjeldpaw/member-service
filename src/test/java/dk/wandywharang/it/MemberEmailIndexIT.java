package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static dk.wandywharang.it.TestRealm.CLUB_A;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The database itself keeps one Member per email, ignoring case as Keycloak does, so no write path can store a second
 * one, whatever the service checks beforehand.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class MemberEmailIndexIT {

    @Inject
    DataSource dataSource;

    @Test
    void aSecondMemberWithTheSameEmailInAnotherCaseIsRefused() throws SQLException {
        TestRealm.seed(dataSource);
        final var member = TestDatabase.insertNewMember(dataSource, "Kim", "Lee", Set.of(CLUB_A));

        final var refused = assertThrows(PSQLException.class, () -> TestDatabase.ensureMember(dataSource,
                UUID.randomUUID(), "Sam", "Doe", (member + "@EXAMPLE.com").toUpperCase(), Set.of(CLUB_A)));

        assertEquals("members_email_lower_key", refused.getServerErrorMessage().getConstraint());
    }
}
