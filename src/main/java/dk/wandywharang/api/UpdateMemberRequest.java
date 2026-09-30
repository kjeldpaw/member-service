package dk.wandywharang.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Builder
public record UpdateMemberRequest(@NotEmpty String firstName, @NotEmpty String lastName, @NotNull Address address, Optional<String> phone, @NotNull @Email String email, Optional<LocalDate> dateOfBirth, @NotNull UUID clubId) {
}
