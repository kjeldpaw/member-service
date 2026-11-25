package dk.wandywharang.it;

import java.util.Map;
import java.util.UUID;

/**
 * The four Clubs that changeset 1.0.0 seeds, with the fixed ids the frontend, tests and support queries rely on.
 */
public final class SeededClubs {
    public static final UUID RANDERS = UUID.fromString("0d09a35b-02c4-4279-9416-a5a302613f3f");
    public static final UUID MIDTDJURS = UUID.fromString("a4f3fcc3-904b-48c0-9b2b-4225dc6dbe92");
    public static final UUID HWA_RANG_AARHUS = UUID.fromString("eced5ba8-4097-4284-b69b-4c8ef24838cc");
    public static final UUID NORDJYSK = UUID.fromString("e52a3058-c086-4588-b7cf-4a30f14a10a1");

    /**
     * The name of each seeded Club, by id.
     */
    public static final Map<UUID, String> NAMES = Map.of(
            RANDERS, "Randers Taekwondo Klub",
            MIDTDJURS, "Midtdjurs Taekwondo Klub",
            HWA_RANG_AARHUS, "Taekwondoklubben Hwa Rang Aarhus",
            NORDJYSK, "Nordjysk Taekwondo Center");

    private SeededClubs() {
    }
}
