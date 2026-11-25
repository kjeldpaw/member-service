package dk.wandywharang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "members")
@Getter
@Setter
public class MemberEntity {
    @Id
    private UUID id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Embedded
    private EmbeddedAddress address;
    private String phone;
    @Column(nullable = false)
    private String email;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "members_clubs",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "club_id"))
    private Set<ClubEntity> clubs = new HashSet<>();
    @OneToOne
    @JoinColumn(name = "graduation_id")
    private GraduationEntity graduation;
    @OneToMany(mappedBy = "member")
    private Set<ReferenceEntity> references;

    /**
     * The ids of the Clubs this Member belongs to.
     */
    public Set<UUID> clubIds() {
        return clubs.stream().map(ClubEntity::getId).collect(Collectors.toSet());
    }

    /**
     * This Member's Graduations, latest first, following each Graduation back to the one before it.
     */
    public List<GraduationEntity> graduationHistory() {
        final var history = new ArrayList<GraduationEntity>();
        for (var current = graduation; current != null; current = current.getPreviousGraduation().orElse(null)) {
            history.add(current);
        }
        return history;
    }
}
