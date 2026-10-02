package dk.wandywharang.service;

import dk.wandywharang.TestData;
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

        assertThrows(ForbiddenException.class, () -> access.findEditable(otherClubMember.getId()).await().indefinitely());
    }

    @Test
    void memberCannotEditMembers() {
        loggedInWith("member");

        assertThrows(ForbiddenException.class, () -> access.findEditable(sameClubMember.getId()).await().indefinitely());
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

        access.requireEditableClub(otherClubMember.getClub().getId()).await().indefinitely();
    }

    @Test
    void instructorCanAddMembersToOwnClub() {
        loggedInWith("instructor");

        access.requireEditableClub(user.getClub().getId()).await().indefinitely();
    }

    @Test
    void instructorCannotAddMembersToOtherClub() {
        loggedInWith("instructor");

        assertThrows(ForbiddenException.class,
                () -> access.requireEditableClub(otherClubMember.getClub().getId()).await().indefinitely());
    }

    @Test
    void memberCannotAddMembers() {
        loggedInWith("member");

        assertThrows(ForbiddenException.class,
                () -> access.requireEditableClub(user.getClub().getId()).await().indefinitely());
    }
}
