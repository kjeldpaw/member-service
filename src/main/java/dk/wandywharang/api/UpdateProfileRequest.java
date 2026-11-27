package dk.wandywharang.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

/**
 * The fields a person may change in their own Profile. There is no email, as it is their Keycloak login, and no Clubs.
 */
@Builder
public record UpdateProfileRequest(@NotEmpty String firstName, @NotEmpty String lastName, @NotNull Address address, Optional<String> phone, Optional<LocalDate> dateOfBirth) {
}
