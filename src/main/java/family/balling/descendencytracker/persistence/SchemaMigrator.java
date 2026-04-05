package family.balling.descendencytracker.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigrator {
    private SchemaMigrator() {
    }

    public static void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS person (
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
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT
                )
                """);

            ensureColumnExists(connection, "person", "fs_pid", "ALTER TABLE person ADD COLUMN fs_pid TEXT");
            ensureColumnExists(connection, "person", "version", "ALTER TABLE person ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
            ensureColumnExists(connection, "person", "sync_status", "ALTER TABLE person ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'");
            ensureColumnExists(connection, "person", "last_synced_at", "ALTER TABLE person ADD COLUMN last_synced_at TEXT");

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_person_active_name
                ON person (is_deleted, surname, given_names, preferred_name)
                """);

            statement.executeUpdate("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_person_fs_pid_unique
                ON person (fs_pid)
                WHERE fs_pid IS NOT NULL
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS parent_child_link (
                    link_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stable_uuid TEXT NOT NULL UNIQUE,
                    parent_person_id INTEGER NOT NULL,
                    child_person_id INTEGER NOT NULL,
                    child_order INTEGER,
                    notes TEXT,
                    is_deleted INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT,
                    FOREIGN KEY(parent_person_id) REFERENCES person(person_id),
                    FOREIGN KEY(child_person_id) REFERENCES person(person_id),
                    UNIQUE(parent_person_id, child_person_id)
                )
                """);
            ensureColumnExists(connection, "parent_child_link", "version", "ALTER TABLE parent_child_link ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
            ensureColumnExists(connection, "parent_child_link", "sync_status", "ALTER TABLE parent_child_link ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'");
            ensureColumnExists(connection, "parent_child_link", "last_synced_at", "ALTER TABLE parent_child_link ADD COLUMN last_synced_at TEXT");

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_parent_child_parent
                ON parent_child_link (parent_person_id, is_deleted, child_order)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_parent_child_child
                ON parent_child_link (child_person_id, is_deleted)
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS spouse_link (
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
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT,
                    FOREIGN KEY(person_a_id) REFERENCES person(person_id),
                    FOREIGN KEY(person_b_id) REFERENCES person(person_id),
                    UNIQUE(person_a_id, person_b_id)
                )
                """);

            ensureColumnExists(
                    connection,
                    "spouse_link",
                    "sealing_to_spouse_status",
                    "ALTER TABLE spouse_link ADD COLUMN sealing_to_spouse_status TEXT NOT NULL DEFAULT 'UNKNOWN'"
            );
            ensureColumnExists(
                    connection,
                    "spouse_link",
                    "sealing_status_date",
                    "ALTER TABLE spouse_link ADD COLUMN sealing_status_date TEXT"
            );
            ensureColumnExists(
                    connection,
                    "spouse_link",
                    "sealing_notes",
                    "ALTER TABLE spouse_link ADD COLUMN sealing_notes TEXT"
            );
            ensureColumnExists(connection, "spouse_link", "version", "ALTER TABLE spouse_link ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
            ensureColumnExists(connection, "spouse_link", "sync_status", "ALTER TABLE spouse_link ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'");
            ensureColumnExists(connection, "spouse_link", "last_synced_at", "ALTER TABLE spouse_link ADD COLUMN last_synced_at TEXT");

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_spouse_link_person_a
                ON spouse_link (person_a_id, is_deleted)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_spouse_link_person_b
                ON spouse_link (person_b_id, is_deleted)
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS person_ordinance_status (
                    person_id INTEGER PRIMARY KEY,
                    baptism_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    confirmation_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    initiatory_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    endowment_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    sealed_to_parents_status TEXT NOT NULL DEFAULT 'UNKNOWN',
                    ordinance_notes TEXT,
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT,
                    FOREIGN KEY(person_id) REFERENCES person(person_id)
                )
                """);
            ensureColumnExists(connection, "person_ordinance_status", "version", "ALTER TABLE person_ordinance_status ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
            ensureColumnExists(connection, "person_ordinance_status", "sync_status", "ALTER TABLE person_ordinance_status ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'");
            ensureColumnExists(connection, "person_ordinance_status", "last_synced_at", "ALTER TABLE person_ordinance_status ADD COLUMN last_synced_at TEXT");

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS line_stewardship (
                    ancestor_person_id INTEGER PRIMARY KEY,
                    stewardship_status TEXT NOT NULL DEFAULT 'UNASSIGNED',
                    notes TEXT,
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT,
                    FOREIGN KEY(ancestor_person_id) REFERENCES person(person_id)
                )
                """);
            ensureColumnExists(connection, "line_stewardship", "version", "ALTER TABLE line_stewardship ADD COLUMN version INTEGER NOT NULL DEFAULT 1");
            ensureColumnExists(connection, "line_stewardship", "sync_status", "ALTER TABLE line_stewardship ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY'");
            ensureColumnExists(connection, "line_stewardship", "last_synced_at", "ALTER TABLE line_stewardship ADD COLUMN last_synced_at TEXT");
        }
    }

    private static void ensureColumnExists(Connection connection, String tableName, String columnName, String alterSql)
            throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {

            while (rs.next()) {
                String existingColumnName = rs.getString("name");
                if (columnName.equalsIgnoreCase(existingColumnName)) {
                    return;
                }
            }
        }

        try (Statement alter = connection.createStatement()) {
            alter.executeUpdate(alterSql);
        }
    }
}
