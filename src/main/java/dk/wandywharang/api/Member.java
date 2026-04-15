package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.UUID;

public interface Member {

    UUID id();

    String firstName();

    String lastName();

    Location location();

    String phone();

    String email();

    LocalDate dateOfBirth();

    String wandyWhaRangId();

    String kukkiWonId();

    Belt belt();
}

