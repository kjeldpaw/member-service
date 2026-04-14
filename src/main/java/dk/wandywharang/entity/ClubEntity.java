package dk.wandywharang.entity;

import dk.wandywharang.api.Club;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clubs")
public class ClubEntity implements Club {

    @Id
    @GeneratedValue
    public Long id;

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
