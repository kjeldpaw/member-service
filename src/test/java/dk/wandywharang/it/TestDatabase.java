package dk.wandywharang.it;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds rows directly through the blocking JDBC datasource (the same one Liquibase uses). Club and belt have no
 * write endpoints, so this is the only way to set up reference data for the reactive member/graduation flows under
 * test.
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static UUID insertClub(DataSource dataSource, String name) throws SQLException {
        final var id = UUID.randomUUID();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO clubs (id, name, street, city, zip_code) VALUES (?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.setString(3, "Main Street 1");
            statement.setString(4, "Aarhus");
            statement.setString(5, "8000");
            statement.executeUpdate();
        }
        return id;
    }

    public static UUID insertBelt(DataSource dataSource, String name, int rank) throws SQLException {
        final var id = UUID.randomUUID();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO belts (id, name, wait_time, rank) VALUES (?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.setLong(3, 0L);
            statement.setInt(4, rank);
            statement.executeUpdate();
        }
        return id;
    }

    /**
     * Inserts the Club unless a Club with that id exists.
     */
    public static void ensureClub(DataSource dataSource, UUID id, String name) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO clubs (id, name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING")) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    /**
     * Inserts a Member who belongs to the given Clubs.
     */
    public static UUID insertMember(DataSource dataSource, Set<UUID> clubIds, String firstName, String lastName)
            throws SQLException {
        final var id = UUID.randomUUID();
        insertMember(dataSource, id, firstName, lastName, id + "@example.com", clubIds);
        return id;
    }

    /**
     * Inserts the Member and links them to the given Clubs, skipping whatever already exists.
     */
    public static void ensureMember(DataSource dataSource, UUID id, String email, Set<UUID> clubIds)
            throws SQLException {
        insertMember(dataSource, id, "Test", "Person", email, clubIds);
    }

    private static void insertMember(DataSource dataSource, UUID id, String firstName, String lastName, String email,
                                     Set<UUID> clubIds) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.prepareStatement(
                    "INSERT INTO members (id, first_name, last_name, email) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT (id) DO NOTHING")) {
                statement.setObject(1, id);
                statement.setString(2, firstName);
                statement.setString(3, lastName);
                statement.setString(4, email);
                statement.executeUpdate();
            }
            for (final var clubId : clubIds) {
                try (var statement = connection.prepareStatement(
                        "INSERT INTO members_clubs (member_id, club_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                    statement.setObject(1, id);
                    statement.setObject(2, clubId);
                    statement.executeUpdate();
                }
            }
        }
    }
}
