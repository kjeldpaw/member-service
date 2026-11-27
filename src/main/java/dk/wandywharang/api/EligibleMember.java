package dk.wandywharang.api;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A Member who is Eligible for a Dan or Poom on a given date. It holds only what planning the Graduation needs, as an
 * Instructor sees the Eligible Members of every Club (ADR 0002): no address, email, phone or date of birth.
 *
 * @param graduationDate the date of the Member's latest Graduation, to their current Belt
 * @param eligibleSince  the date of the latest Graduation plus the current Belt's wait time, which can be up to one
 *                       calendar month after the given date
 */
@Builder
public record EligibleMember(UUID id,
                             String firstName,
                             String lastName,
                             List<Club> clubs,
                             Belt belt,
                             LocalDate graduationDate,
                             Belt nextBelt,
                             LocalDate eligibleSince) {
}
