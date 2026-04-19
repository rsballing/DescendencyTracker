package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRelationshipRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void addParentChildRestoresSoftDeletedRow() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("parent-child.db"));
        databaseManager.initialize();

        SqlitePersonRepository personRepository = new SqlitePersonRepository(databaseManager);
        SqliteRelationshipRepository relationshipRepository = new SqliteRelationshipRepository(databaseManager);

        Person parent = personRepository.save(person("Parent"));
        Person child = personRepository.save(person("Child"));

        ParentChildLink original = relationshipRepository.addParentChild(parent.getPersonId(), child.getPersonId(), 1, "first");
        relationshipRepository.softDeleteParentChild(original.getLinkId());

        ParentChildLink restored = relationshipRepository.addParentChild(parent.getPersonId(), child.getPersonId(), 2, "restored");

        assertEquals(original.getLinkId(), restored.getLinkId());
        assertEquals(2, restored.getChildOrder());
        assertEquals("restored", restored.getNotes());
        assertFalse(restored.isDeleted());
        assertEquals(1, countRows(databaseManager, "parent_child_link"));
    }

    @Test
    void addSpouseRestoresSoftDeletedRow() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("spouse.db"));
        databaseManager.initialize();

        SqlitePersonRepository personRepository = new SqlitePersonRepository(databaseManager);
        SqliteRelationshipRepository relationshipRepository = new SqliteRelationshipRepository(databaseManager);

        Person first = personRepository.save(person("First"));
        Person second = personRepository.save(person("Second"));

        SpouseLink original = relationshipRepository.addSpouse(
                first.getPersonId(),
                second.getPersonId(),
                "1900",
                "first",
                OrdinanceStatus.UNKNOWN,
                false,
                null,
                null
        );
        relationshipRepository.softDeleteSpouse(original.getSpouseLinkId());

        SpouseLink restored = relationshipRepository.addSpouse(
                second.getPersonId(),
                first.getPersonId(),
                "1901",
                "restored",
                OrdinanceStatus.COMPLETE,
                true,
                "1902",
                "sealed"
        );

        assertEquals(original.getSpouseLinkId(), restored.getSpouseLinkId());
        assertEquals("1901", restored.getMarriageDateText());
        assertEquals("restored", restored.getMarriageNotes());
        assertEquals(OrdinanceStatus.COMPLETE, restored.getSealingToSpouseStatus());
        assertTrue(restored.isSealedToSpouseReserved());
        assertFalse(restored.isDeleted());
        assertEquals(1, countRows(databaseManager, "spouse_link"));
    }

    @Test
    void schemaMigrationCreatesLineStewardshipTable() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("schema.db"));
        databaseManager.initialize();

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(line_stewardship)")) {

            boolean foundAncestorPersonId = false;
            boolean foundStewardshipStatus = false;
            boolean foundNotes = false;
            boolean foundUpdatedAt = false;

            while (rs.next()) {
                String columnName = rs.getString("name");
                foundAncestorPersonId |= "ancestor_person_id".equals(columnName);
                foundStewardshipStatus |= "stewardship_status".equals(columnName);
                foundNotes |= "notes".equals(columnName);
                foundUpdatedAt |= "updated_at".equals(columnName);
            }

            assertTrue(foundAncestorPersonId);
            assertTrue(foundStewardshipStatus);
            assertTrue(foundNotes);
            assertTrue(foundUpdatedAt);
        }
    }

    private Person person(String name) {
        Person person = new Person();
        person.setPreferredName(name);
        return person;
    }

    private int countRows(DatabaseManager databaseManager, String tableName) throws Exception {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
