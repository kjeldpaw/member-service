package dk.wandywharang.api;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface UpdateMemberGraduationRequest {

    LocalDate getDate();

    Set<UUID> getGraduatedBy();

    UUID getBeltId();

}
