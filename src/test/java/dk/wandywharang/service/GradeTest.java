package dk.wandywharang.service;

import dk.wandywharang.service.Grade.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeTest {

    @Test
    void readsTheGradeAtTheEndOfABeltsName() {
        assertEquals(Optional.of(new Grade(Kind.KUP, 1)), Grade.of("Red Belt with three black snippets (1. KUP)"));
        assertEquals(Optional.of(new Grade(Kind.POOM, 3)), Grade.of("Black and red Belt (3. POOM)"));
        assertEquals(Optional.of(new Grade(Kind.DAN, 9)), Grade.of("Black Belt (9. DAN)"));
        assertEquals(Optional.of(new Grade(Kind.MON, 9)), Grade.of("Yellow Belt (9. MON)"));
        assertEquals(Optional.of(new Grade(Kind.DAN, 2)), Grade.of("Black Belt (2. dan) "));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "White Belt", "Test Belt 1", "(1. KUP) Red Belt"})
    void aNameWithoutAGradeAtTheEndHasNone(String name) {
        assertEquals(Optional.empty(), Grade.of(name));
    }

    @ParameterizedTest(name = "{0} {1}, under fifteen = {2} -> {3}")
    @CsvSource(nullValues = "-", value = {
            "KUP,  1, true,  1. POOM",
            "KUP,  1, false, 1. DAN",
            "POOM, 1, true,  2. POOM",
            "POOM, 3, true,  4. POOM",
            "POOM, 2, false, 3. DAN",
            "POOM, 4, true,  -",
            "POOM, 4, false, 5. DAN",
            "DAN,  1, true,  2. DAN",
            "DAN,  1, false, 2. DAN",
            "DAN,  9, false, 10. DAN",
            "KUP,  2, false, -",
            "KUP, 10, true,  -",
            "MON,  1, true,  -",
    })
    void nextBlackBelt(Kind kind, int number, boolean underFifteen, String next) {
        final var expected = Optional.ofNullable(next)
                .map(text -> text.split("\\. "))
                .map(parts -> new Grade(Kind.valueOf(parts[1]), Integer.parseInt(parts[0])));

        assertEquals(expected, new Grade(kind, number).nextBlackBelt(underFifteen));
    }
}
