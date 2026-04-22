package dk.wandywharang.entity;

import dk.wandywharang.api.Club;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "clubs")
public class ClubEntity implements Club {

    @Id
    @GeneratedValue
    private UUID id;
    @Column(unique = true, nullable = false)
    private String name;
    @Embedded
    public Address address;

    public ClubEntity(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    public ClubEntity() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public dk.wandywharang.api.Address getAddress() {
        return address;
    }
}
