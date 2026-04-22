package dk.wandywharang.entity;

import dk.wandywharang.api.Reference;
import dk.wandywharang.api.ReferenceType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "references")
public class ReferenceEntity implements Reference {
    @Id
    @GeneratedValue
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReferenceType type;
    @Column(name = "reference", nullable = false)
    private String reference;
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    public ReferenceEntity() {
    }

    public ReferenceEntity(ReferenceType type, String reference) {
        this.type = type;
        this.reference = reference;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ReferenceType getType() {
        return type;
    }

    public void setType(ReferenceType type) {
        this.type = type;
    }

    @Override
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public MemberEntity getMember() {
        return member;
    }
}

