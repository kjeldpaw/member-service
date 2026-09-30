package dk.wandywharang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.UUID;

@Getter
@Entity
@Table(name = "belts")
public class BeltEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = DurationDaysConverter.class)
    @Column(name = "wait_time", nullable = false)
    private Duration waitTime;

    @Column(nullable = false)
    private Integer rank;
}
