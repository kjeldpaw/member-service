package dk.wandywharang.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

public record Examiner(@NotEmpty UUID id,
                       @NotEmpty String firstName,
                       @NotEmpty String lastName) {
}
