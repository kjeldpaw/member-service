package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;

public record BeltRecord(BeltType type, LocalDate graduationDate, String graduatedBy, Optional<Belt> previousBelt) implements Belt {
}
