package dk.wandywharang.api;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Builder
public record CreateMemberRequest(String firstName, String lastName, Address address, Optional<String> phone, String email, Optional<LocalDate> dateOfBirth, UUID clubId) {


}
