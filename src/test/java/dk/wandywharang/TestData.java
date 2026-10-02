package dk.wandywharang;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.Club;
import dk.wandywharang.api.Member;
import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds entities and API objects for tests. Entities with a generated id have no setter for it, so the id is set by
 * reflection.
 */
public final class TestData {

    private TestData() {
    }

    public static ClubEntity club() {
        return withId(new ClubEntity());
    }

    public static MemberEntity member(ClubEntity club) {
        final var member = new MemberEntity();
        member.setId(UUID.randomUUID());
        member.setFirstName("Kim");
        member.setLastName("Lee");
        member.setEmail("kim@example.com");
        member.setClub(club);
        member.setReferences(new HashSet<>());
        return member;
    }

    public static BeltEntity belt() {
        return withId(new BeltEntity());
    }

    public static GraduationEntity graduation(GraduationEntity previous) {
        final var graduation = withId(new GraduationEntity());
        graduation.setDate(LocalDate.of(2026, 6, 1));
        graduation.setGraduatedBy(new HashSet<>());
        graduation.setPreviousGraduation(previous);
        return graduation;
    }

    public static Member member() {
        return Member.builder()
                .id(UUID.randomUUID())
                .firstName("Kim")
                .lastName("Lee")
                .address(new Address("Main Street 1", "Aarhus", "8000"))
                .phone(Optional.empty())
                .email("kim@example.com")
                .dateOfBirth(Optional.empty())
                .club(new Club(UUID.randomUUID(), "Taekwondo Club", null))
                .graduation(Optional.empty())
                .references(Set.of())
                .build();
    }

    public static <T> T withId(T entity) {
        try {
            final var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
