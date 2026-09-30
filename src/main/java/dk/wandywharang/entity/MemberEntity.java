package dk.wandywharang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

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
    @OneToOne
    @JoinColumn(name = "club_id", nullable = false)
    private ClubEntity club;
    @OneToOne
    @JoinColumn(name = "graduation_id")
    private GraduationEntity graduation;
    @OneToMany(mappedBy = "member")
    private Set<ReferenceEntity> references;
}