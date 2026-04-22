package dk.wandywharang.entity;

import dk.wandywharang.api.Belt;
import jakarta.persistence.*;

import java.time.Duration;
import java.util.UUID;

@Entity
@Table(name = "belts")
public class BeltEntity implements Belt {

    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(name = "wait_time", nullable = false)
    private Duration waitTime;
    @Column(nullable = false)
    private Integer rank;

    public BeltEntity() {
    }

    public BeltEntity(String name, Duration waitTime, Integer rank) {
        this.name = name;
        this.waitTime = waitTime;
        this.rank = rank;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Duration getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Duration waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
