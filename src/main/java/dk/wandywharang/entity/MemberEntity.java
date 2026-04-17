package dk.wandywharang.entity;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.Member;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "members")
public class MemberEntity implements Member {

    @Id
    public UUID id;

    public String firstName;

    public String lastName;

    @Embedded
    public Location location;

    public String phone;

    public String email;

    public LocalDate dateOfBirth;

    public String wandyWhaRangId;

    public String kukkiWonId;

    @OneToOne(targetEntity = BeltEntity.class)
    @JoinColumn(name = "belt_id")
    public Belt belt;

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String firstName() {
        return firstName;
    }

    @Override
    public String lastName() {
        return lastName;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public String phone() {
        return phone;
    }

    @Override
    public String email() {
        return email;
    }

    @Override
    public LocalDate dateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public String wandyWhaRangId() {
        return wandyWhaRangId;
    }

    @Override
    public String kukkiWonId() {
        return kukkiWonId;
    }

    @Override
    public Belt belt() {
        return belt;
    }
}
