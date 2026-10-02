package dk.wandywharang.it;

import javax.sql.DataSource;
import java.sql.SQLException;
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

    public static void insertMember(DataSource dataSource, UUID id, UUID clubId, String firstName, String lastName,
                                     String email) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "INSERT INTO members (id, first_name, last_name, email, club_id) VALUES (?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, email);
            statement.setObject(5, clubId);
            statement.executeUpdate();
        }
    }
}
