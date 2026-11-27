package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAccessTest {
    @Mock
    SecurityIdentity securityIdentity;
    @Mock
    MemberRepository repository;

    MemberAccess access;
    MemberEntity user;
    MemberEntity sameClubMember;
    MemberEntity otherClubMember;

    @BeforeEach
    void setUp() {
        access = new MemberAccess(securityIdentity, repository);
        final var club = TestData.club();
        user = TestData.member(club);
        sameClubMember = TestData.member(club);
        otherClubMember = TestData.member(TestData.club());

        lenient().when(repository.findById(any(UUID.class))).thenReturn(Uni.createFrom().nullItem());
        for (final var member : Set.of(user, sameClubMember, otherClubMember)) {
            lenient().when(repository.findById(member.getId())).thenReturn(Uni.createFrom().item(member));
        }
        final Principal principal = () -> user.getId().toString();
        lenient().when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    private void loggedInWith(String... roles) {
        lenient().when(securityIdentity.hasRole(anyString()))
                .thenAnswer(invocation -> Set.of(roles).contains(invocation.<String>getArgument(0)));
    }

    @Test
    void adminCanEditMemberOfAnyClub() {
        loggedInWith("admin");

        assertSame(otherClubMember, access.findEditable(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void instructorCanEditMemberOfOwnClub() {
        loggedInWith("instructor");

        assertSame(sameClubMember, access.findEditable(sameClubMember.getId()).await().indefinitely());
    }

    @Test
    void instructorCannotEditMemberOfOtherClub() {
        loggedInWith("instructor");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.findEditable(otherClubMember.getId()).await().indefinitely());
        assertEquals(String.format("Member with id = %s shares no Club with you", otherClubMember.getId()),
                failure.getMessage());
    }

    @Test
    void plainMemberCannotEditMembers() {
        loggedInWith("member");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.findEditable(sameClubMember.getId()).await().indefinitely());
        assertEquals("Not allowed to edit Members", failure.getMessage());
    }

    @Test
    void editingUnknownMemberIsNotFound() {
        loggedInWith("admin");

        assertThrows(NotFoundException.class, () -> access.findEditable(UUID.randomUUID()).await().indefinitely());
    }

    @Test
    void plainMemberCanSeeThemselves() {
        loggedInWith("member");

        assertSame(user, access.findVisible(user.getId()).await().indefinitely());
    }

    @Test
    void plainMemberIsToldMemberOfOwnClubIsNotFound() {
        loggedInWith("member");

        assertThrows(NotFoundException.class, () -> access.findVisible(sameClubMember.getId()).await().indefinitely());
    }

    @Test
    void instructorCanSeeMemberOfOwnClub() {
        loggedInWith("member", "instructor");

        assertSame(sameClubMember, access.findVisible(sameClubMember.getId()).await().indefinitely());
    }

    @Test
    void memberOfOtherClubIsHiddenFromInstructorAsNotFound() {
        loggedInWith("member", "instructor");

        assertThrows(NotFoundException.class, () -> access.findVisible(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void plainMemberListsOnlyThemselves() {
        loggedInWith("member");

        assertEquals(List.of(user), access.findAllVisible().await().indefinitely());
    }

    @Test
    void instructorListsMembersSharingAClub() {
        loggedInWith("member", "instructor");
        when(repository.findSharingClubWith(user)).thenReturn(Uni.createFrom().item(List.of(user, sameClubMember)));

        assertEquals(List.of(user, sameClubMember), access.findAllVisible().await().indefinitely());
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminListsAllMembers() {
        loggedInWith("admin");
        final PanacheQuery<MemberEntity> all = mock(PanacheQuery.class);
        when(all.list()).thenReturn(Uni.createFrom().item(List.of(user, sameClubMember, otherClubMember)));
        when(repository.findAll()).thenReturn(all);

        assertEquals(List.of(user, sameClubMember, otherClubMember), access.findAllVisible().await().indefinitely());
    }

    @Test
    void adminCanSeeMemberOfAnyClub() {
        loggedInWith("admin");

        assertSame(otherClubMember, access.findVisible(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void adminCanAddMembersToAnyClub() {
        loggedInWith("admin");

        access.requireEditableClubs(otherClubMember.clubIds()).await().indefinitely();
    }

    @Test
    void instructorCanAddMembersToOwnClub() {
        loggedInWith("instructor");

        access.requireEditableClubs(user.clubIds()).await().indefinitely();
    }

    @Test
    void instructorCannotAddMembersToOtherClub() {
        loggedInWith("instructor");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.requireEditableClubs(otherClubMember.clubIds()).await().indefinitely());
        assertEquals(String.format("Clubs with ids = %s are not your Clubs", otherClubMember.clubIds()),
                failure.getMessage());
    }

    @Test
    void plainMemberCannotAddMembers() {
        loggedInWith("member");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.requireEditableClubs(user.clubIds()).await().indefinitely());
        assertEquals("Not allowed to edit Members", failure.getMessage());
    }

    @Test
    void callerIsTheLoggedInPersonsIdAndEmail() {
        loggedInAs(user.getId(), "kim@example.com");

        assertEquals(new Caller(user.getId(), "kim@example.com"), access.caller().await().indefinitely());
    }

    @Test
    void callerWithoutAnEmailIsABadRequest() {
        loggedInAs(user.getId(), null);

        final var failure = assertThrows(BadRequestException.class, () -> access.caller().await().indefinitely());
        assertEquals("The token has no email", failure.getMessage());
    }

    private void loggedInAs(UUID id, String email) {
        final var token = mock(JsonWebToken.class);
        lenient().when(token.getName()).thenReturn(id.toString());
        lenient().when(token.<String>getClaim(Claims.email)).thenReturn(email);
        when(securityIdentity.getPrincipal()).thenReturn(token);
    }
}
