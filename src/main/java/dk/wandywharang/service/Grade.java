package dk.wandywharang.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The Mon, Kup, Poom or Dan grade and number of a Belt, read from the end of its name, as in
 * "Red Belt with three black snippets (1. KUP)".
 */
public record Grade(Kind kind, int number) {
    private static final Pattern NAME = Pattern.compile("\\((\\d+)\\. (MON|KUP|POOM|DAN)\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final int HIGHEST_POOM = 4;

    public enum Kind { MON, KUP, POOM, DAN }

    /**
     * The grade at the end of a Belt's name, or empty if the name has none.
     */
    public static Optional<Grade> of(String beltName) {
        if (beltName == null) {
            return Optional.empty();
        }
        final var match = NAME.matcher(beltName);
        if (!match.find()) {
            return Optional.empty();
        }
        return Optional.of(new Grade(Kind.valueOf(match.group(2).toUpperCase(Locale.ROOT)),
                Integer.parseInt(match.group(1))));
    }

    /**
     * The Dan or Poom a Member holding this grade goes for next: a Poom when they are under 15 on the day of the
     * Graduation, otherwise a Dan. Empty when this grade leads to no Dan or Poom: a Mon, a Kup below 1. Kup, or
     * 4. Poom for a Member under 15. Whether the next Dan exists as a Belt is up to the caller.
     */
    public Optional<Grade> nextBlackBelt(boolean underFifteen) {
        final var next = switch (kind) {
            case MON -> null;
            case KUP -> number == 1 ? 1 : null;
            case POOM, DAN -> number + 1;
        };
        if (next == null) {
            return Optional.empty();
        }
        if (kind == Kind.DAN || !underFifteen) {
            return Optional.of(new Grade(Kind.DAN, next));
        }
        return next <= HIGHEST_POOM ? Optional.of(new Grade(Kind.POOM, next)) : Optional.empty();
    }
}
