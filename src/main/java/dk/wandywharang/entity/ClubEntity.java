package dk.wandywharang.entity;

import dk.wandywharang.api.Club;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "clubs")
public class ClubEntity implements Club {

    @Id
    @GeneratedValue
    public UUID id;

    public String name;

    @Embedded
    public Location location;

    @Override
    public String name() {
        return name;
    }

    @Override
    public Location location() {
        return location;
    }
}
