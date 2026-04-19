package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteUpgradeMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void initializeUpgradesExistingDatabaseWithoutLosingData() throws Exception {
        Path dbPath = tempDir.resolve("legacy.db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
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
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT
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
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT
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
                    updated_at TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    sync_status TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                    last_synced_at TEXT
                )
                """);

            statement.executeUpdate("""
                INSERT INTO person (
                    person_id, stable_uuid, fs_pid, preferred_name, sex, is_living, birth_date_precision, death_date_precision,
                    reviewed_status, is_root, is_deleted, created_at, updated_at, version, sync_status
                ) VALUES (
                    1, 'person-1', 'ABCD-123', 'Legacy Person', 'UNKNOWN', 0, 'UNKNOWN', 'UNKNOWN',
                    'NOT_REVIEWED', 0, 0, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', 1, 'LOCAL_ONLY'
                )
                """);
            statement.executeUpdate("""
                INSERT INTO person (
                    person_id, stable_uuid, fs_pid, preferred_name, sex, is_living, birth_date_precision, death_date_precision,
                    reviewed_status, is_root, is_deleted, created_at, updated_at, version, sync_status
                ) VALUES (
                    2, 'person-2', NULL, 'Legacy Spouse', 'UNKNOWN', 0, 'UNKNOWN', 'UNKNOWN',
                    'NOT_REVIEWED', 0, 0, '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', 1, 'LOCAL_ONLY'
                )
                """);
            statement.executeUpdate("""
                INSERT INTO spouse_link (
                    spouse_link_id, stable_uuid, person_a_id, person_b_id, marriage_date_text, marriage_notes,
                    sealing_to_spouse_status, sealing_status_date, sealing_notes, is_deleted, created_at, updated_at, version, sync_status
                ) VALUES (
                    1, 'spouse-1', 1, 2, '1900', 'legacy marriage', 'OPEN', '1901', 'legacy sealing', 0,
                    '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z', 1, 'LOCAL_ONLY'
                )
                """);
            statement.executeUpdate("""
                INSERT INTO person_ordinance_status (
                    person_id, baptism_status, confirmation_status, initiatory_status, endowment_status,
                    sealed_to_parents_status, ordinance_notes, updated_at, version, sync_status
                ) VALUES (
                    1, 'OPEN', 'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 'legacy notes',
                    '2026-01-01T00:00:00Z', 1, 'LOCAL_ONLY'
                )
                """);
        }

        DatabaseManager databaseManager = new DatabaseManager(dbPath);
        databaseManager.initialize();

        SqlitePersonRepository personRepository = new SqlitePersonRepository(databaseManager);
        SqliteRelationshipRepository relationshipRepository = new SqliteRelationshipRepository(databaseManager);
        SqliteOrdinanceRepository ordinanceRepository = new SqliteOrdinanceRepository(databaseManager);

        Person person = personRepository.findById(1L).orElseThrow();
        PersonOrdinanceStatus ordinanceStatus = ordinanceRepository.findByPersonId(1L).orElseThrow();
        SpouseLink spouseLink = relationshipRepository.findSpousesForPerson(1L).getFirst();

        assertEquals("Legacy Person", person.getPreferredName());
        assertFalse(person.isConfirmedNoChildren());
        assertFalse(person.isConfirmedNoSpouse());
        assertEquals(OrdinanceStatus.OPEN, ordinanceStatus.getBaptismStatus());
        assertFalse(ordinanceStatus.isBaptismReserved());
        assertEquals("legacy notes", ordinanceStatus.getOrdinanceNotes());
        assertEquals(OrdinanceStatus.OPEN, spouseLink.getSealingToSpouseStatus());
        assertFalse(spouseLink.isSealedToSpouseReserved());
    }

    @Test
    void repositoriesSaveAndReloadNewFields() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("new-fields.db"));
        databaseManager.initialize();

        SqlitePersonRepository personRepository = new SqlitePersonRepository(databaseManager);
        SqliteOrdinanceRepository ordinanceRepository = new SqliteOrdinanceRepository(databaseManager);
        SqliteRelationshipRepository relationshipRepository = new SqliteRelationshipRepository(databaseManager);

        Person first = new Person();
        first.setPreferredName("First");
        first.setConfirmedNoChildren(true);
        first.setConfirmedNoSpouse(true);
        first = personRepository.save(first);

        Person second = new Person();
        second.setPreferredName("Second");
        second = personRepository.save(second);

        PersonOrdinanceStatus ordinanceStatus = new PersonOrdinanceStatus();
        ordinanceStatus.setPersonId(first.getPersonId());
        ordinanceStatus.setBaptismReserved(true);
        ordinanceStatus.setConfirmationReserved(true);
        ordinanceStatus.setEndowmentReserved(true);
        ordinanceRepository.save(ordinanceStatus);

        relationshipRepository.addSpouse(
                first.getPersonId(),
                second.getPersonId(),
                "1900",
                null,
                OrdinanceStatus.UNKNOWN,
                true,
                null,
                null
        );

        Person reloadedPerson = personRepository.findById(first.getPersonId()).orElseThrow();
        PersonOrdinanceStatus reloadedStatus = ordinanceRepository.findByPersonId(first.getPersonId()).orElseThrow();
        SpouseLink spouseLink = relationshipRepository.findSpousesForPerson(first.getPersonId()).getFirst();

        assertTrue(reloadedPerson.isConfirmedNoChildren());
        assertTrue(reloadedPerson.isConfirmedNoSpouse());
        assertTrue(reloadedStatus.isBaptismReserved());
        assertTrue(reloadedStatus.isConfirmationReserved());
        assertTrue(reloadedStatus.isEndowmentReserved());
        assertTrue(spouseLink.isSealedToSpouseReserved());
    }
}
