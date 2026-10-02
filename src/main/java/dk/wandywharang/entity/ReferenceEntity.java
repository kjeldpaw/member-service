package dk.wandywharang.entity;

import dk.wandywharang.api.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "`references`")
public class ReferenceEntity {

    @Getter
    @Id
    @GeneratedValue
    private UUID id;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReferenceType type;

    @Getter
    @Setter
    @Column(name = "reference", nullable = false)
    private String reference;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
}

