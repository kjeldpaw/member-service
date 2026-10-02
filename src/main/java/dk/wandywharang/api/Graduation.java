package dk.wandywharang.api;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Builder
public record Graduation(UUID id, LocalDate date, Set<Examiner> graduatedBy, Belt belt, Optional<Graduation> previousGraduation) {
}
