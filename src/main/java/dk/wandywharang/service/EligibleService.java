package dk.wandywharang.service;

import dk.wandywharang.api.EligibleMember;
import io.smallrye.mutiny.Uni;

import java.time.LocalDate;
import java.util.List;

public interface EligibleService {

    /**
     * Finds the Members of every Club who are Eligible for a Dan or Poom on the date, ordered by last name and then
     * first name.
     */
    Uni<List<EligibleMember>> findEligible(LocalDate date);
}
