package dk.wandywharang.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Builder
public record Member(@NotEmpty UUID id,
                     @NotEmpty String firstName,
                     @NotEmpty String lastName,
                     @NotNull Address address,
                     Optional<String> phone,
                     @NotNull @Email String email,
                     Optional<LocalDate> dateOfBirth,
                     @NotNull List<Club> clubs,
                     Optional<Graduation> graduation,
                     Set<Reference> references,
                     @JsonInclude(JsonInclude.Include.NON_ABSENT) Optional<Role> role) {

    /**
     * Only an Admin sees a Member's Role, so it is left out unless the service adds it with {@link #withRole}.
     */
    public Member {
        if (role == null) {
            role = Optional.empty();
        }
    }

    public Member withRole(Role role) {
        return new Member(id, firstName, lastName, address, phone, email, dateOfBirth, clubs, graduation, references, Optional.of(role));
    }
}
