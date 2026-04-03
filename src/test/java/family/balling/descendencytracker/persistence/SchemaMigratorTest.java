package family.balling.descendencytracker.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaMigratorTest {

    @Test
    void migrateAddsSyncAndStewardshipColumnsToExistingSchema() throws Exception {
        Path dbPath = Files.createTempFile("descendency-migrate", ".db");
        Class.forName("org.sqlite.JDBC");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE person (
                    person_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stable_uuid TEXT NOT NULL UNIQUE,
                    fs_pid TEXT,
                    preferred_name TEXT NOT NULL,
                    given_names TEXT,
                    surname TEXT,
                    sex TEXT NOT NULL DEFAULT 'UNKNOWN',
                    is_living INTEGER NOT NULL DEFAULT 0,
                    birth_date_text TEXT,
                    death_date_text TEXT,
                    birth_date_precision TEXT NOT NULL DEFAULT 'UNKNOWN',
                    death_date_precision TEXT NOT NULL DEFAULT 'UNKNOWN',
                    reviewed_status TEXT NOT NULL DEFAULT 'NOT_REVIEWED',
                    notes TEXT,
                    is_root INTEGER NOT NULL DEFAULT 0,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE parent_child_link (
                    link_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stable_uuid TEXT NOT NULL UNIQUE,
                    parent_person_id INTEGER NOT NULL,
                    child_person_id INTEGER NOT NULL,
                    child_order INTEGER,
                    notes TEXT,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE spouse_link (
                    spouse_link_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stable_uuid TEXT NOT NULL UNIQUE,
                    person_a_id INTEGER NOT NULL,
                    person_b_id INTEGER NOT NULL,
                    marriage_date_text TEXT,
                    marriage_notes TEXT,
                    sealing_to_spouse_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    sealing_status_date TEXT,
                    sealing_notes TEXT,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE person_ordinance_status (
                    person_id INTEGER PRIMARY KEY,
                    baptism_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    confirmation_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    initiatory_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    endowment_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    sealed_to_parents_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    ordinance_notes TEXT,
                    updated_at TEXT NOT NULL
                )
                """);

            SchemaMigrator.migrate(connection);

            assertTrue(columnExists(connection, "person", "stewardship_status"));
            assertTrue(columnExists(connection, "person", "sync_status"));
            assertTrue(columnExists(connection, "person", "version"));
            assertTrue(columnExists(connection, "parent_child_link", "sync_status"));
            assertTrue(columnExists(connection, "spouse_link", "last_modified_by_device"));
            assertTrue(columnExists(connection, "person_ordinance_status", "last_synced_at"));
        } finally {
            Files.deleteIfExists(dbPath);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
