package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface Member {

    String getId();

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

