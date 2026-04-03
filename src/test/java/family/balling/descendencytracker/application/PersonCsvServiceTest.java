package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import family.balling.descendencytracker.domain.enums.StewardshipStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;
import family.balling.descendencytracker.persistence.DatabaseManager;
import family.balling.descendencytracker.persistence.SqlitePersonRepository;
import family.balling.descendencytracker.repository.PersonRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersonCsvServiceTest {

    @Test
    void exportAndImportPreserveStewardshipAndSyncMetadata() throws Exception {
        Path sourceDb = Files.createTempFile("descendency-source", ".db");
        Path targetDb = Files.createTempFile("descendency-target", ".db");
        Path csvPath = Files.createTempFile("descendency-people", ".csv");

        try {
            DatabaseManager sourceManager = new DatabaseManager(sourceDb);
            sourceManager.initialize();

            PersonRepository sourceRepository = new SqlitePersonRepository(sourceManager);
            PersonService sourcePersonService = new PersonService(sourceRepository);
            PersonCsvService exportService = new PersonCsvService(sourceRepository, sourcePersonService);

            Person person = new Person();
            person.setStableUuid("uuid-123");
            person.setPreferredName("Ada Lovelace");
            person.setGivenNames("Augusta Ada");
            person.setSurname("Lovelace");
            person.setSex(Sex.FEMALE);
            person.setReviewedStatus(ReviewedStatus.REVIEWED);
            person.setLastReviewedOn("2026-04-02T10:00:00Z");
            person.setStewardshipStatus(StewardshipStatus.SHARED);
            person.setNotes("Imported via CSV");
            person.setVersion(7);
            person.setSyncStatus(SyncStatus.SYNCED);
            person.setLastSyncedAt("2026-04-01T09:00:00Z");
            person.setLastModifiedByDevice("desktop-alpha");
            sourcePersonService.saveImportedPerson(person);

            exportService.exportPeople(csvPath);

            DatabaseManager targetManager = new DatabaseManager(targetDb);
            targetManager.initialize();

            PersonRepository targetRepository = new SqlitePersonRepository(targetManager);
            PersonService targetPersonService = new PersonService(targetRepository);
            PersonCsvService importService = new PersonCsvService(targetRepository, targetPersonService);

            assertEquals(1, importService.importPeople(csvPath));

            Person imported = targetRepository.findByStableUuid("uuid-123").orElseThrow();
            assertEquals("Ada Lovelace", imported.getPreferredName());
            assertEquals(StewardshipStatus.SHARED, imported.getStewardshipStatus());
            assertEquals(SyncStatus.SYNCED, imported.getSyncStatus());
            assertEquals(7, imported.getVersion());
            assertEquals("desktop-alpha", imported.getLastModifiedByDevice());
            assertNotNull(imported.getCreatedAt());
            assertNotNull(imported.getUpdatedAt());
        } finally {
            Files.deleteIfExists(sourceDb);
            Files.deleteIfExists(targetDb);
            Files.deleteIfExists(csvPath);
        }
    }
}
