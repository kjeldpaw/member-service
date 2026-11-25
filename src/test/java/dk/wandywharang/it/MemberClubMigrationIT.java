package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Migrates a database that still has the single Club column on Members, as development databases do, and checks
 * that every Member keeps their Club. The migration runs in a schema of its own, so it does not disturb the schema
 * the application uses.
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@Tag("integration")
class MemberClubMigrationIT {
    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Inject
    DataSource dataSource;

    @Test
    void migratingMembersWithASingleClubLinksThemThroughTheJoinTableAndDropsTheColumn() throws Exception {
        final var schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection()) {
            execute(connection, "CREATE SCHEMA " + schema);
            execute(connection, "SET search_path TO " + schema);
            try {
                final var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(schema);
                final var liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);

                // Only the first changeset, 1.0.0, which has the single Club column
                liquibase.update(1, "");
                final var randers = UUID.fromString("0d09a35b-02c4-4279-9416-a5a302613f3f");
                final var aarhus = UUID.fromString("eced5ba8-4097-4284-b69b-4c8ef24838cc");
                final var kim = insertSingleClubMember(connection, randers);
                final var sam = insertSingleClubMember(connection, aarhus);

                liquibase.update("");

                assertEquals(Map.of(kim, randers, sam, aarhus), clubLinks(connection));
                assertFalse(hasColumn(connection, schema, "members", "club_id"));
            } finally {
                execute(connection, "RESET search_path");
                execute(connection, "DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static UUID insertSingleClubMember(Connection connection, UUID clubId) throws SQLException {
        final var id = UUID.randomUUID();
        try (var statement = connection.prepareStatement(
                "INSERT INTO members (id, first_name, last_name, email, club_id) VALUES (?, ?, ?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, "Kim");
            statement.setString(3, "Lee");
            statement.setString(4, id + "@example.com");
            statement.setObject(5, clubId);
            statement.executeUpdate();
        }
        return id;
    }

    private static Map<UUID, UUID> clubLinks(Connection connection) throws SQLException {
        final var links = new HashMap<UUID, UUID>();
        try (var statement = connection.prepareStatement("SELECT member_id, club_id FROM members_clubs");
             var result = statement.executeQuery()) {
            while (result.next()) {
                links.put(result.getObject(1, UUID.class), result.getObject(2, UUID.class));
            }
        }
        return links;
    }

    private static boolean hasColumn(Connection connection, String schema, String table, String column)
            throws SQLException {
        try (var result = connection.getMetaData().getColumns(null, schema, table, column)) {
            return result.next();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
