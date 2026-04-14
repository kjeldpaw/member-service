package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;

public interface Belt {

    BeltType type();

    LocalDate graduationDate();

    String graduatedBy();

    Optional<Belt> previousBelt();
}
