package dk.wandywharang.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Creates the caller's own Member record. There is no email, as it comes from the caller's token, and the Clubs may
 * be any Clubs, as long as there is at least one.
 */
@Builder
public record CreateProfileRequest(@NotEmpty String firstName, @NotEmpty String lastName, @NotNull Address address, Optional<String> phone, Optional<LocalDate> dateOfBirth, @NotEmpty Set<@NotNull UUID> clubIds) {
}
