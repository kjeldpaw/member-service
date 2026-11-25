package dk.wandywharang.it;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The people in wandywharang-realm.json and the Clubs their Member records sit in, so the access rules can be
 * exercised across shared and unshared Clubs:
 *
 * <ul>
 *     <li>{@link #ADMIN} has no Member record.</li>
 *     <li>{@link #INSTRUCTOR_AB} is an Instructor in Club A and Club B.</li>
 *     <li>{@link #INSTRUCTOR_C} is an Instructor in Club C.</li>
 *     <li>{@link #MEMBER_A} is a Member of Club A.</li>
 *     <li>{@link #MEMBER_BC} is a Member of Club B and Club C.</li>
 * </ul>
 *
 * <p>The Clubs have fixed ids and the Member records are keyed by the Keycloak subject, so {@link #seed} is
 * idempotent and can run before every test.
 */
public final class TestRealm {
    public static final UUID CLUB_A = UUID.fromString("6a1f0c3e-0d6b-4a43-9a4e-0000000000a1");
    public static final UUID CLUB_B = UUID.fromString("6a1f0c3e-0d6b-4a43-9a4e-0000000000b2");
    public static final UUID CLUB_C = UUID.fromString("6a1f0c3e-0d6b-4a43-9a4e-0000000000c3");

    public static final Person ADMIN = new Person("admin@wandywharang.dk", "admin", Set.of());
    public static final Person INSTRUCTOR_AB = new Person("instructor@wandywharang.dk", "instructor", Set.of(CLUB_A, CLUB_B));
    public static final Person INSTRUCTOR_C = new Person("instructor-c@wandywharang.dk", "instructor", Set.of(CLUB_C));
    public static final Person MEMBER_A = new Person("member@wandywharang.dk", "member", Set.of(CLUB_A));
    public static final Person MEMBER_BC = new Person("member-bc@wandywharang.dk", "member", Set.of(CLUB_B, CLUB_C));

    private static final Map<String, UUID> SUBJECTS = new ConcurrentHashMap<>();
    private static final Set<Person> MEMBERS = Set.of(INSTRUCTOR_AB, INSTRUCTOR_C, MEMBER_A, MEMBER_BC);

    private TestRealm() {
    }

    public static void seed(DataSource dataSource) throws SQLException {
        TestDatabase.ensureClub(dataSource, CLUB_A, "Test Club A");
        TestDatabase.ensureClub(dataSource, CLUB_B, "Test Club B");
        TestDatabase.ensureClub(dataSource, CLUB_C, "Test Club C");
        for (final var person : MEMBERS) {
            TestDatabase.ensureMember(dataSource, person.id(), "Test", "Person", person.username(), person.clubs());
        }
    }

    /**
     * A person who can log in to the test realm, with the Clubs of their Member record.
     */
    public record Person(String username, String password, Set<UUID> clubs) {

        public String token() {
            return KeycloakTestSupport.tokenFor(username, password);
        }

        /**
         * The Keycloak subject, which is also the id of the person's Member record.
         */
        public UUID id() {
            return SUBJECTS.computeIfAbsent(username, ignored -> KeycloakTestSupport.subjectOf(token()));
        }
    }
}
