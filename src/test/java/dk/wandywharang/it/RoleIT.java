package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dk.wandywharang.it.MemberRequests.newEmail;
import static dk.wandywharang.it.TestRealm.CLUB_A;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Admins give Members a Role, held as Keycloak realm roles (ADR 0003). The application calls Keycloak as its own
 * service account here, so these tests also show that the account has the rights the Role needs.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class RoleIT {
    private static final Set<String> ROLE_REALM_ROLES = Set.of("member", "instructor", "admin");

    @Inject
    DataSource dataSource;

    @BeforeEach
    void seed() throws SQLException {
        TestRealm.seed(dataSource);
    }

    private static Map<String, Object> memberBody(String email, String role) {
        final var body = new HashMap<>(MemberRequests.memberBody(email, CLUB_A));
        body.put("role", role);
        return body;
    }

    /**
     * The realm roles of the user that decide their Role, leaving out the realm's other default roles.
     */
    private static Set<String> roleRealmRolesOf(UUID id) {
        return onlyRoleRealmRoles(KeycloakTestSupport.realmRolesOf(id));
    }

    private static Set<String> onlyRoleRealmRoles(Set<String> realmRoles) {
        return realmRoles.stream().filter(ROLE_REALM_ROLES::contains).collect(Collectors.toSet());
    }

    private static void changeRole(String token, Object id, String role, int status) {
        given()
                .auth().oauth2(token)
                .contentType("application/json").body(Map.of("role", role))
                .put("/api/v1/members/{id}/role", id)
                .then().statusCode(status);
    }

    /**
     * A person who can log in, with the lowest Role and a Member record in Club A.
     */
    private TestRealm.Person newMember() throws SQLException {
        final var person = TestRealm.newPerson("member");
        TestDatabase.ensureMember(dataSource, person.id(), "New", "Person", person.username(), Set.of(CLUB_A));
        return person;
    }

    @Test
    void theApplicationCallsKeycloakAsItsServiceAccount() {
        assertEquals("member-service", KeycloakTestSupport.adminClientId());
    }

    @Test
    void adminCreatesMemberWithRoleAndKeycloakHoldsIt() {
        final var adminToken = TestRealm.ADMIN.token();

        for (final var role : Set.of("instructor", "admin")) {
            final String id = given()
                    .auth().oauth2(adminToken)
                    .contentType("application/json").body(memberBody(newEmail(), role))
                    .post("/api/v1/members")
                    .then().statusCode(201)
                    .body("role", equalTo(role))
                    .extract().path("id");

            final var expected = role.equals("admin") ? ROLE_REALM_ROLES : Set.of("member", "instructor");
            assertEquals(expected, roleRealmRolesOf(UUID.fromString(id)));
            given()
                    .auth().oauth2(adminToken)
                    .get("/api/v1/members/{id}", id)
                    .then().statusCode(200)
                    .body("role", equalTo(role));
        }
    }

    @Test
    void memberCreatedWithoutRoleIsAPlainMember() {
        final var id = MemberRequests.register(newEmail(), CLUB_A);

        assertEquals(Set.of("member"), roleRealmRolesOf(UUID.fromString(id)));
        given()
                .auth().oauth2(TestRealm.ADMIN.token())
                .get("/api/v1/members/{id}", id)
                .then().statusCode(200)
                .body("role", equalTo("member"));
    }

    @Test
    void instructorCannotCreateMemberWithAHigherRoleAndNothingIsCreated() {
        for (final var role : Set.of("instructor", "admin")) {
            final var email = newEmail();

            given()
                    .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                    .contentType("application/json").body(memberBody(email, role))
                    .post("/api/v1/members")
                    .then().statusCode(403);

            assertFalse(KeycloakTestSupport.userExists(email));
        }
    }

    @Test
    void instructorCreatesPlainMemberWithoutSeeingTheRole() {
        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .contentType("application/json").body(memberBody(newEmail(), "member"))
                .post("/api/v1/members")
                .then().statusCode(201)
                .body("$", not(hasKey("role")));
    }

    @Test
    void adminChangesRoleUpAndDownAndTheNextTokenCarriesIt() throws SQLException {
        final var person = newMember();
        final var adminToken = TestRealm.ADMIN.token();

        for (final var role : new String[]{"admin", "member", "instructor", "admin", "instructor", "member"}) {
            given()
                    .auth().oauth2(adminToken)
                    .contentType("application/json").body(Map.of("role", role))
                    .put("/api/v1/members/{id}/role", person.id())
                    .then().statusCode(200)
                    .body("id", equalTo(person.id().toString()))
                    .body("role", equalTo(role));

            final Set<String> expected = switch (role) {
                case "admin" -> ROLE_REALM_ROLES;
                case "instructor" -> Set.of("member", "instructor");
                default -> Set.of("member");
            };
            assertEquals(expected, roleRealmRolesOf(person.id()), "after changing to " + role);
            assertEquals(expected, onlyRoleRealmRoles(KeycloakTestSupport.realmRolesIn(person.token())),
                    "token after changing to " + role);
            given()
                    .auth().oauth2(adminToken)
                    .get("/api/v1/members/{id}", person.id())
                    .then().statusCode(200)
                    .body("role", equalTo(role));
        }
    }

    @Test
    void anInstructorWhoBecomesAnAdminSeesEveryClub() throws SQLException {
        final var person = newMember();
        final var otherClubMember = TestDatabase.insertNewMember(dataSource, "Other", "Club", Set.of(TestRealm.CLUB_C));

        given()
                .auth().oauth2(person.token())
                .get("/api/v1/members/{id}", otherClubMember)
                .then().statusCode(404);

        changeRole(TestRealm.ADMIN.token(), person.id(), "admin", 200);

        given()
                .auth().oauth2(person.token())
                .get("/api/v1/members/{id}", otherClubMember)
                .then().statusCode(200)
                // A Member without a Keycloak user is shown without a Role
                .body("$", not(hasKey("role")));
    }

    @Test
    void adminCannotChangeTheirOwnRole() throws SQLException {
        final var person = newMember();
        changeRole(TestRealm.ADMIN.token(), person.id(), "admin", 200);

        changeRole(person.token(), person.id(), "member", 403);

        assertEquals(ROLE_REALM_ROLES, roleRealmRolesOf(person.id()));
    }

    @Test
    void onlyAdminsChangeRoles() throws SQLException {
        final var person = newMember();

        changeRole(TestRealm.INSTRUCTOR_AB.token(), person.id(), "instructor", 403);
        changeRole(TestRealm.MEMBER_A.token(), person.id(), "instructor", 403);

        assertEquals(Set.of("member"), roleRealmRolesOf(person.id()));
    }

    @Test
    void changingToAnUnknownRoleIsABadRequest() throws SQLException {
        changeRole(TestRealm.ADMIN.token(), newMember().id(), "superuser", 400);
    }

    @Test
    void changingTheRoleOfAnUnknownMemberIsNotFound() {
        changeRole(TestRealm.ADMIN.token(), UUID.randomUUID(), "instructor", 404);
    }

    @Test
    void instructorDoesNotSeeTheRoleOfAMember() {
        given()
                .auth().oauth2(TestRealm.INSTRUCTOR_AB.token())
                .get("/api/v1/members/{id}", TestRealm.MEMBER_A.id())
                .then().statusCode(200)
                .body("$", not(hasKey("role")));
    }
}
