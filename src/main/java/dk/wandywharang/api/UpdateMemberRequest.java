package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface UpdateMemberRequest {

    String getFirstName();

    String getLastName();

    Address getAddress();

    Optional<String> getPhone();

    Optional<LocalDate> getDateOfBirth();

    Set<? extends Reference> getReferences();

}
