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

    public String name;

    @Embedded
    public Location location;

    public String phone;

    public String email;

    public LocalDate dateOfBirth;

    public String wandyWhaRangId;

    public String kukkiWonId;

    @Column(unique = true, nullable = false)
    public String keycloakId; // the "sub" claim from the JWT

    @OneToOne(targetEntity = BeltEntity.class)
    public Belt belt;

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String name() {
        return name;
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
