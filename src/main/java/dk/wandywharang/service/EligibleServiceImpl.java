package dk.wandywharang.service;

import dk.wandywharang.api.EligibleMember;
import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.repository.BeltRepository;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A Member is Eligible for their next Belt on a date when, by one calendar month after that date, they will have held
 * their current Belt for at least its wait time. Only a Dan or Poom counts as a next Belt here; which one follows
 * from {@link Grade#nextBlackBelt}, with the Member's age taken on the date itself. A Member whose date of birth is not
 * known goes for a Dan.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class EligibleServiceImpl implements EligibleService {
    static final int FIFTEEN = 15;

    private final MemberRepository memberRepository;
    private final BeltRepository beltRepository;
    private final BeltMapper beltMapper;
    private final ClubMapper clubMapper;

    @Override
    @WithSession
    public Uni<List<EligibleMember>> findEligible(LocalDate date) {
        return beltRepository.findAll().list()
                .chain(belts -> memberRepository.findGraduated()
                        .map(members -> eligible(members, belts, date)));
    }

    List<EligibleMember> eligible(List<MemberEntity> members, List<BeltEntity> belts, LocalDate date) {
        final Map<Grade, BeltEntity> beltsByGrade = belts.stream()
                .filter(belt -> Grade.of(belt.getName()).isPresent())
                .sorted(Comparator.comparing(BeltEntity::getRank))
                .collect(Collectors.toMap(belt -> Grade.of(belt.getName()).orElseThrow(), Function.identity(),
                        (lower, higher) -> lower));
        return members.stream()
                .map(member -> eligible(member, beltsByGrade, date))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<EligibleMember> eligible(MemberEntity member, Map<Grade, BeltEntity> beltsByGrade, LocalDate date) {
        final var graduation = member.getGraduation();
        if (graduation == null) {
            return Optional.empty();
        }
        final var belt = graduation.getBelt();
        final var eligibleSince = graduation.getDate().plusDays(belt.getWaitTime().toDays());
        if (eligibleSince.isAfter(date.plusMonths(1))) {
            return Optional.empty();
        }
        return Grade.of(belt.getName())
                .flatMap(grade -> grade.nextBlackBelt(isUnderFifteen(member, date)))
                .map(beltsByGrade::get)
                .map(nextBelt -> EligibleMember.builder()
                        .id(member.getId())
                        .firstName(member.getFirstName())
                        .lastName(member.getLastName())
                        .clubs(clubMapper.map(member.getClubs()))
                        .belt(beltMapper.map(belt))
                        .graduationDate(graduation.getDate())
                        .nextBelt(beltMapper.map(nextBelt))
                        .eligibleSince(eligibleSince)
                        .build());
    }

    private static boolean isUnderFifteen(MemberEntity member, LocalDate date) {
        final var dateOfBirth = member.getDateOfBirth();
        return dateOfBirth != null && date.isBefore(dateOfBirth.plusYears(FIFTEEN));
    }
}
