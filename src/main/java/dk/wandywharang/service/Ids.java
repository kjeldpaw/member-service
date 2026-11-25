package dk.wandywharang.service;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

final class Ids {

    private Ids() {
    }

    /**
     * The given ids that are not among the others, sorted so messages that list them are stable.
     */
    static SortedSet<UUID> notIn(Collection<UUID> ids, Collection<UUID> others) {
        final var missing = new TreeSet<>(ids);
        missing.removeAll(others);
        return missing;
    }
}
