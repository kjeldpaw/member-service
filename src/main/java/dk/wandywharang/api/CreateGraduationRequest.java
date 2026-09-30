package dk.wandywharang.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Builder
public record CreateGraduationRequest(@NotNull LocalDate date, @NotNull Set<UUID> graduatedBy, @NotNull UUID beltId) {
}
