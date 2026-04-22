package dk.wandywharang.entity;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.Member;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "graduations")
public class GraduationEntity implements Graduation {

    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private LocalDate date;
    @ManyToMany
    @JoinTable(name = "graduation_members",
            joinColumns = @JoinColumn(name = "graduation_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<MemberEntity> graduatedBy;
    @OneToOne
    @JoinColumn(name = "belt_id", nullable = false)
    private BeltEntity belt;
    @OneToOne
    @JoinColumn(name = "graduation_id")
    private GraduationEntity previousGraduation;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public Set<? extends  Member> getGraduatedBy() {
        return graduatedBy;
    }

    public void setGraduatedBy(Set<MemberEntity> graduatedBy) {
        this.graduatedBy = graduatedBy;
    }

    @Override
    public Belt getBelt() {
        return belt;
    }

    public void setBelt(BeltEntity belt) {
        this.belt = belt;
    }

    @Override
    public Optional<? extends Graduation> getPreviousGraduation() {
        return Optional.ofNullable(previousGraduation);
    }

    public void setPreviousGraduation(GraduationEntity previousGraduation) {
        this.previousGraduation = previousGraduation;
    }
}
