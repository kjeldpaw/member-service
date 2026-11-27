package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Belt;
import dk.wandywharang.api.Club;
import dk.wandywharang.api.EligibleMember;
import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.repository.BeltRepository;
import dk.wandywharang.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EligibleServiceImplTest {
    private static final LocalDate GRADUATION_DAY = LocalDate.of(2027, 3, 15);

    private static final BeltEntity KUP_2 = TestData.belt("Red Belt with two black snippets (2. KUP)", 0, 18);
    private static final BeltEntity KUP_1 = TestData.belt("Red Belt with three black snippets (1. KUP)", 365, 19);
    private static final BeltEntity POOM_1 = TestData.belt("Black and red Belt (1. POOM)", 365, 20);
    private static final BeltEntity POOM_2 = TestData.belt("Black and red Belt (2. POOM)", 730, 21);
    private static final BeltEntity POOM_4 = TestData.belt("Black and red Belt (4. POOM)", 1460, 23);
    private static final BeltEntity DAN_1 = TestData.belt("Black Belt (1. DAN)", 365, 24);
    private static final BeltEntity DAN_2 = TestData.belt("Black Belt (2. DAN)", 730, 25);
    private static final BeltEntity DAN_3 = TestData.belt("Black Belt (3. DAN)", 1096, 26);
    private static final BeltEntity DAN_5 = TestData.belt("Black Belt (5. DAN)", 1825, 28);
    private static final BeltEntity DAN_9 = TestData.belt("Black Belt (9. DAN)", 3285, 32);
    private static final BeltEntity TEST_BELT = TestData.belt("Test Belt", 0, 100);
    private static final List<BeltEntity> BELTS =
            List.of(KUP_2, KUP_1, POOM_1, POOM_2, POOM_4, DAN_1, DAN_2, DAN_3, DAN_5, DAN_9, TEST_BELT);

    @Mock
    MemberRepository memberRepository;
    @Mock
    BeltRepository beltRepository;
    @Mock
    BeltMapper beltMapper;
    @Mock
    ClubMapper clubMapper;

    EligibleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EligibleServiceImpl(memberRepository, beltRepository, beltMapper, clubMapper);
        when(beltMapper.map(any(BeltEntity.class))).thenAnswer(invocation -> {
            final BeltEntity belt = invocation.getArgument(0);
            return new Belt(belt.getId(), belt.getName(), belt.getWaitTime(), belt.getRank());
        });
        when(clubMapper.map(anySet())).thenReturn(List.of(new Club(UUID.randomUUID(), "Taekwondo Club", null)));
    }

    @Test
    void aMemberWhoseWaitEndedBeforeTheDateIsEligibleForTheNextDan() {
        final var member = member(DAN_1, LocalDate.of(2025, 1, 10), null);

        final var eligible = only(service.eligible(List.of(member), BELTS, GRADUATION_DAY));

        assertEquals(member.getId(), eligible.id());
        assertEquals("Kim", eligible.firstName());
        assertEquals("Lee", eligible.lastName());
        assertEquals(List.of("Taekwondo Club"), eligible.clubs().stream().map(Club::name).toList());
        assertEquals(DAN_1.getId(), eligible.belt().id());
        assertEquals(LocalDate.of(2025, 1, 10), eligible.graduationDate());
        assertEquals(DAN_2.getId(), eligible.nextBelt().id());
        assertEquals(LocalDate.of(2026, 1, 10), eligible.eligibleSince());
    }

    @Test
    void aMemberWhoseWaitEndsUpToOneCalendarMonthAfterTheDateIsEligible() {
        // 1. Dan waits 365 days: graduated 2026-04-15, the wait ends 2027-04-15, one month after 2027-03-15
        final var lastDay = member(DAN_1, LocalDate.of(2026, 4, 15), null);
        final var oneDayLate = member(DAN_1, LocalDate.of(2026, 4, 16), null);

        final var eligible = service.eligible(List.of(lastDay, oneDayLate), BELTS, GRADUATION_DAY);

        assertEquals(List.of(lastDay.getId()), eligible.stream().map(EligibleMember::id).toList());
        assertEquals(LocalDate.of(2027, 4, 15), eligible.getFirst().eligibleSince());
    }

    @Test
    void oneMonthAfterTheThirtyFirstOfJanuaryIsTheEndOfFebruary() {
        final var date = LocalDate.of(2027, 1, 31);
        // Waits end 2027-02-28 and 2027-03-01
        final var lastDay = member(DAN_1, LocalDate.of(2026, 2, 28), null);
        final var oneDayLate = member(DAN_1, LocalDate.of(2026, 3, 1), null);

        final var eligible = service.eligible(List.of(lastDay, oneDayLate), BELTS, date);

        assertEquals(List.of(lastDay.getId()), eligible.stream().map(EligibleMember::id).toList());
    }

    @Test
    void underFifteenOnTheDateGoesForAPoomAndFifteenOrOlderForADan() {
        final var fourteen = member(KUP_1, LocalDate.of(2025, 1, 1), GRADUATION_DAY.minusYears(15).plusDays(1));
        final var fifteen = member(KUP_1, LocalDate.of(2025, 1, 1), GRADUATION_DAY.minusYears(15));

        final var eligible = service.eligible(List.of(fourteen, fifteen), BELTS, GRADUATION_DAY);

        assertEquals(List.of(POOM_1.getId(), DAN_1.getId()),
                eligible.stream().map(member -> member.nextBelt().id()).toList());
    }

    @Test
    void ageIsTakenOnTheDateEvenWhenTheWaitEndsLater() {
        // Turns 15 a week after the date, within the month the wait may still run
        final var member = member(KUP_1, LocalDate.of(2026, 4, 1), GRADUATION_DAY.minusYears(15).plusWeeks(1));

        assertEquals(POOM_1.getId(), only(service.eligible(List.of(member), BELTS, GRADUATION_DAY)).nextBelt().id());
    }

    @Test
    void aMemberWithoutADateOfBirthGoesForADan() {
        final var kup = member(KUP_1, LocalDate.of(2025, 1, 1), null);
        final var poom = member(POOM_2, LocalDate.of(2024, 1, 1), null);

        final var eligible = service.eligible(List.of(kup, poom), BELTS, GRADUATION_DAY);

        assertEquals(List.of(DAN_1.getId(), DAN_3.getId()),
                eligible.stream().map(member -> member.nextBelt().id()).toList());
    }

    @Test
    void aPoomHolderUnderFifteenGoesForTheNextPoomAndFromFifteenForTheNextDan() {
        final var young = member(POOM_1, LocalDate.of(2025, 1, 1), LocalDate.of(2015, 1, 1));
        final var older = member(POOM_1, LocalDate.of(2025, 1, 1), LocalDate.of(2010, 1, 1));

        final var eligible = service.eligible(List.of(young, older), BELTS, GRADUATION_DAY);

        assertEquals(List.of(POOM_2.getId(), DAN_2.getId()),
                eligible.stream().map(member -> member.nextBelt().id()).toList());
    }

    @Test
    void fourthPoomIsNotEligibleUnderFifteenAndGoesForFifthDanFromFifteen() {
        final var young = member(POOM_4, LocalDate.of(2020, 1, 1), LocalDate.of(2013, 1, 1));
        final var older = member(POOM_4, LocalDate.of(2020, 1, 1), LocalDate.of(2010, 1, 1));

        final var eligible = service.eligible(List.of(young, older), BELTS, GRADUATION_DAY);

        assertEquals(List.of(older.getId()), eligible.stream().map(EligibleMember::id).toList());
        assertEquals(DAN_5.getId(), eligible.getFirst().nextBelt().id());
    }

    @Test
    void membersWhoseNextBeltIsNoDanOrPoomOrWhoHaveNoGraduationAreNotEligible() {
        final var topDan = member(DAN_9, LocalDate.of(2000, 1, 1), null);
        final var secondKup = member(KUP_2, LocalDate.of(2020, 1, 1), null);
        final var unknownBelt = member(TEST_BELT, LocalDate.of(2020, 1, 1), null);
        final var noGraduation = TestData.member(TestData.club());

        assertEquals(List.of(), service.eligible(List.of(topDan, secondKup, unknownBelt, noGraduation), BELTS,
                GRADUATION_DAY));
    }

    private static MemberEntity member(BeltEntity belt, LocalDate graduated, LocalDate dateOfBirth) {
        final var member = TestData.member(TestData.club());
        member.setId(UUID.randomUUID());
        member.setDateOfBirth(dateOfBirth);
        final var graduation = TestData.graduation(null);
        graduation.setBelt(belt);
        graduation.setDate(graduated);
        member.setGraduation(graduation);
        return member;
    }

    private static EligibleMember only(List<EligibleMember> eligible) {
        assertEquals(1, eligible.size(), () -> "Expected one Eligible Member, got " + eligible);
        return eligible.getFirst();
    }
}
