package dk.wandywharang.api.record;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.Member;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record GraduationRecord(UUID id,
                               LocalDate date,
                               Set<Member> graduatedBy,
                               Belt belt,
                               Graduation previousGraduation) implements Graduation {

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public Set<? extends Member> getGraduatedBy() {
        return graduatedBy != null ? graduatedBy : Set.of();
    }

    @Override
    public Belt getBelt() {
        return belt;
    }

    @Override
    public Optional<? extends Graduation> getPreviousGraduation() {
        return Optional.ofNullable(previousGraduation);
    }
}
