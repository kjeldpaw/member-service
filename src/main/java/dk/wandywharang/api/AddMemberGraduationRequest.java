package dk.wandywharang.api;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Builder
public record AddMemberGraduationRequest(LocalDate date, Set<UUID> graduatedBy, UUID beltId) {
}
