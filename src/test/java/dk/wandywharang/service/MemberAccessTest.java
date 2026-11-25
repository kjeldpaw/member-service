package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

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

    private static Set<UUID> clubIdsOf(MemberEntity member) {
        return member.getClubs().stream().map(ClubEntity::getId).collect(Collectors.toSet());
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
    void memberCannotEditMembers() {
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
    void memberCanSeeMemberOfOwnClub() {
        loggedInWith("member");

        assertSame(sameClubMember, access.findVisible(sameClubMember.getId()).await().indefinitely());
    }

    @Test
    void memberOfOtherClubIsHiddenAsNotFound() {
        loggedInWith("member");

        assertThrows(NotFoundException.class, () -> access.findVisible(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void adminCanSeeMemberOfAnyClub() {
        loggedInWith("admin");

        assertSame(otherClubMember, access.findVisible(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void adminCanAddMembersToAnyClub() {
        loggedInWith("admin");

        access.requireEditableClubs(clubIdsOf(otherClubMember)).await().indefinitely();
    }

    @Test
    void instructorCanAddMembersToOwnClub() {
        loggedInWith("instructor");

        access.requireEditableClubs(clubIdsOf(user)).await().indefinitely();
    }

    @Test
    void instructorCannotAddMembersToOtherClub() {
        loggedInWith("instructor");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.requireEditableClubs(clubIdsOf(otherClubMember)).await().indefinitely());
        assertEquals(String.format("Clubs with ids = %s are not your Clubs", clubIdsOf(otherClubMember)),
                failure.getMessage());
    }

    @Test
    void memberCannotAddMembers() {
        loggedInWith("member");

        final var failure = assertThrows(ForbiddenException.class,
                () -> access.requireEditableClubs(clubIdsOf(user)).await().indefinitely());
        assertEquals("Not allowed to edit Members", failure.getMessage());
    }
}
