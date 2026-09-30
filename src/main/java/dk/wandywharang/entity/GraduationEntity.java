package dk.wandywharang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "graduations")
public class GraduationEntity {

    @Getter
    @Id
    @GeneratedValue
    private UUID id;

    @Getter
    @Setter
    @Column(nullable = false)
    private LocalDate date;

    @Getter
    @Setter
    @ManyToMany
    @JoinTable(name = "graduation_members",
            joinColumns = @JoinColumn(name = "graduation_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<MemberEntity> graduatedBy;


    @Getter
    @Setter
    @OneToOne
    @JoinColumn(name = "belt_id", nullable = false)
    private BeltEntity belt;

    @Setter
    @OneToOne
    @JoinColumn(name = "graduation_id")
    private GraduationEntity previousGraduation;


    public Optional<GraduationEntity> getPreviousGraduation() {
        return Optional.ofNullable(previousGraduation);
    }
}
