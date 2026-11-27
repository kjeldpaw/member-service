package dk.wandywharang.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What a Member may do. Each Role is held as Keycloak realm roles, and includes the Roles before it: see ADR 0003.
 * On the wire it is the name of its highest realm role.
 */
public enum Role {
    @JsonProperty("member") MEMBER("member"),
    @JsonProperty("instructor") INSTRUCTOR("instructor"),
    @JsonProperty("admin") ADMIN("admin");

    private final String realmRole;

    Role(String realmRole) {
        this.realmRole = realmRole;
    }

    /**
     * The realm role that marks this Role, such as {@code instructor}.
     */
    public String realmRole() {
        return realmRole;
    }

    /**
     * The realm roles a Member with this Role holds: their own and those of every Role before it.
     */
    public Set<String> realmRoles() {
        return Stream.of(values())
                .filter(role -> role.ordinal() <= ordinal())
                .map(Role::realmRole)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The highest Role whose realm role is among the given ones, or {@link #MEMBER} if there is none.
     */
    public static Role highestOf(Set<String> realmRoles) {
        return Stream.of(values())
                .filter(role -> realmRoles.contains(role.realmRole()))
                .max(Comparator.naturalOrder())
                .orElse(MEMBER);
    }
}
