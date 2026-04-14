package dk.wandywharang.entity;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.BeltType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Optional;

@Entity
@Table(name = "belts")
public class BeltEntity implements Belt {

    @Id
    @GeneratedValue
    public Long id;

    public BeltType type;

    public LocalDate graduationDate;

    public String graduatedBy;

    @OneToOne(targetEntity = BeltEntity.class, fetch = FetchType.LAZY)
    public BeltEntity previousBelt;

    @Override
    @Enumerated(EnumType.STRING)
    public BeltType type() {
        return type;
    }

    @Override
    public LocalDate graduationDate() {
        return graduationDate;
    }

    @Override
    public String graduatedBy() {
        return graduatedBy;
    }

    @Override
    public Optional<Belt> previousBelt() {
        return Optional.ofNullable(previousBelt);
    }
}
