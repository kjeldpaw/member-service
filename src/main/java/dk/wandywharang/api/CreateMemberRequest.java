package dk.wandywharang.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A new Member. The Role is optional and defaults to {@link Role#MEMBER}; only an Admin may ask for a higher one.
 */
@Builder
public record CreateMemberRequest(@NotEmpty String firstName, @NotEmpty String lastName, @NotNull Address address, Optional<String> phone, @NotNull @Email String email, Optional<LocalDate> dateOfBirth, @NotEmpty Set<@NotNull UUID> clubIds, Role role) {

    public CreateMemberRequest {
        if (role == null) {
            role = Role.MEMBER;
        }
    }
}
