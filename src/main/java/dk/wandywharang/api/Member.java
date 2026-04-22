package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Member {

    UUID getId();

    String getFirstName();

    String getLastName();

    Address getAddress();

    String getPhone();

    String getEmail();

    LocalDate getDateOfBirth();

    Club getClub();

    Optional<? extends Graduation> getGraduation();

    Set<? extends Reference> getReferences();

}

