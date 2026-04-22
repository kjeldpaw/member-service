package dk.wandywharang.entity;

import dk.wandywharang.api.Club;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.Reference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "members")
public class MemberEntity implements Member {

    @Id
    @Column(length = 50)
    private String id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Embedded
    private Address address;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String email;
    @Column(name= "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @OneToOne
    @JoinColumn(name = "club_id", nullable = false)
    public ClubEntity club;
    @OneToOne
    @JoinColumn(name = "graduation_id")
    public GraduationEntity graduation;
    @OneToMany(mappedBy = "member")
    private Set<ReferenceEntity> references;

    public MemberEntity() {
    }

    public MemberEntity(String firstName, String lastName, Address address, String phone, String email, LocalDate dateOfBirth, ClubEntity club, GraduationEntity graduation, Set<ReferenceEntity> references) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.club = club;
        this.graduation = graduation;
        this.references = references;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public Club getClub() {
        return club;
    }

    public void setClub(ClubEntity club) {
        this.club = club;
    }

    @Override
    public Optional<? extends Graduation> getGraduation() {
        return Optional.ofNullable(graduation);
    }

    public void setGraduation(GraduationEntity graduation) {
        this.graduation = graduation;
    }

    @Override
    public Set<? extends Reference> getReferences() {
        return references != null ? references : Set.of();
    }

    public void setReferences(Set<ReferenceEntity> references) {
        this.references = references;
    }
}
