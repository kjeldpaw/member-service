package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Graduation {

    UUID getId();

    LocalDate getDate();

    Set<? extends  Member> getGraduatedBy();

    Belt getBelt();

    Optional<? extends Graduation> getPreviousGraduation();
}
