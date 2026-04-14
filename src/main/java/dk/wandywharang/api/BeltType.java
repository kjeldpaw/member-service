package dk.wandywharang.api;

import java.time.Duration;

public enum BeltType {
    KUP_10TH("10. KUP", Duration.ofDays(182)),
    KUP_9TH("9. KUP", Duration.ofDays(182)),
    KUP_8TH("8. KUP", Duration.ofDays(182)),
    KUP_7TH("7. KUP", Duration.ofDays(182)),
    KUP_6TH("6. KUP", Duration.ofDays(182)),
    KUP_5TH("5. KUP", Duration.ofDays(182)),
    KUP_4TH("4. KUP", Duration.ofDays(182)),
    KUP_3RD("3. KUP", Duration.ofDays(182)),
    KUP_2ND("2. KUP", Duration.ofDays(182)),
    KUP_1ST("1. KUP", Duration.ofDays(365)),
    DAN_1ST("1. DAN", Duration.ofDays(365)),
    DAN_2ND("2. DAN", Duration.ofDays(730)),
    DAN_3RD("3. DAN", Duration.ofDays(1095));

    private final String displayName;
    private final Duration waitForNextBelt;

    BeltType(String displayName, Duration waitForNextBelt) {
        this.displayName = displayName;
        this.waitForNextBelt = waitForNextBelt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Duration getWaitForNextBelt() {
        return waitForNextBelt;
    }
}
