package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.UUID;

public record MemberRecord(UUID id, String name, Location location, String phone, String email, LocalDate dateOfBirth, String wandyWhaRangId, String kukkiWonId, Belt belt) implements Member {
}
