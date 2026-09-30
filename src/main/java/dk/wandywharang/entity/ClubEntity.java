package dk.wandywharang.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "clubs")
@Getter
public class ClubEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Embedded
    public EmbeddedAddress address;

}
