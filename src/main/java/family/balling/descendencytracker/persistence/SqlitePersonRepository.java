package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.enums.DatePrecision;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import family.balling.descendencytracker.domain.enums.SyncStatus;
import family.balling.descendencytracker.repository.PersonRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqlitePersonRepository implements PersonRepository {
    private final DatabaseManager databaseManager;

    public SqlitePersonRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<Person> findAllActive() {
        String sql = """
            SELECT person_id,
                   stable_uuid,
                   fs_pid,
                   preferred_name,
                   given_names,
                   surname,
                   sex,
                   is_living,
                   birth_date_text,
                   death_date_text,
                   birth_date_precision,
                   death_date_precision,
                   reviewed_status,
                   notes,
                   is_root,
                   is_deleted,
                   created_at,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM person
            WHERE is_deleted = 0
            ORDER BY is_root DESC,
                     COALESCE(surname, '') COLLATE NOCASE,
                     COALESCE(given_names, '') COLLATE NOCASE,
                     COALESCE(preferred_name, '') COLLATE NOCASE
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            List<Person> people = new ArrayList<>();
            while (rs.next()) {
                people.add(mapRow(rs));
            }
            return people;
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load people.", ex);
        }
    }

    @Override
    public Optional<Person> findById(long personId) {
        String sql = """
            SELECT person_id,
                   stable_uuid,
                   fs_pid,
                   preferred_name,
                   given_names,
                   surname,
                   sex,
                   is_living,
                   birth_date_text,
                   death_date_text,
                   birth_date_precision,
                   death_date_precision,
                   reviewed_status,
                   notes,
                   is_root,
                   is_deleted,
                   created_at,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM person
            WHERE person_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, personId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load person.", ex);
        }
    }

    @Override
    public Optional<Person> findRootPerson() {
        String sql = """
            SELECT person_id,
                   stable_uuid,
                   fs_pid,
                   preferred_name,
                   given_names,
                   surname,
                   sex,
                   is_living,
                   birth_date_text,
                   death_date_text,
                   birth_date_precision,
                   death_date_precision,
                   reviewed_status,
                   notes,
                   is_root,
                   is_deleted,
                   created_at,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM person
            WHERE is_deleted = 0
              AND is_root = 1
            LIMIT 1
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load root person.", ex);
        }
    }

    @Override
    public Person save(Person person) {
        if (person.getPersonId() == null) {
            return insert(person);
        }
        return update(person);
    }

    @Override
    public void softDelete(long personId) {
        String sql = """
            UPDATE person
            SET is_deleted = 1,
                version = version + 1,
                sync_status = 'LOCAL_ONLY',
                last_synced_at = NULL,
                is_root = 0,
                updated_at = ?
            WHERE person_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, Instant.now().toString());
            statement.setLong(2, personId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not delete person.", ex);
        }
    }

    @Override
    public void setRootPerson(long personId) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement clearStatement = connection.prepareStatement("""
                    UPDATE person
                    SET is_root = 0,
                        version = version + 1,
                        sync_status = 'LOCAL_ONLY',
                        last_synced_at = NULL,
                        updated_at = ?
                    WHERE is_deleted = 0
                    """);
                 PreparedStatement setStatement = connection.prepareStatement("""
                    UPDATE person
                    SET is_root = 1,
                        version = version + 1,
                        sync_status = 'LOCAL_ONLY',
                        last_synced_at = NULL,
                        updated_at = ?
                    WHERE person_id = ?
                      AND is_deleted = 0
                    """)) {

                String now = Instant.now().toString();

                clearStatement.setString(1, now);
                clearStatement.executeUpdate();

                setStatement.setString(1, now);
                setStatement.setLong(2, personId);
                int updated = setStatement.executeUpdate();

                if (updated != 1) {
                    throw new IllegalArgumentException("Could not set the selected person as root.");
                }

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not update the root person.", ex);
        }
    }

    private Person insert(Person person) {
        String sql = """
            INSERT INTO person (
                stable_uuid,
                fs_pid,
                preferred_name,
                given_names,
                surname,
                sex,
                is_living,
                birth_date_text,
                death_date_text,
                birth_date_precision,
                death_date_precision,
                reviewed_status,
                notes,
                is_root,
                is_deleted,
                created_at,
                updated_at,
                version,
                sync_status,
                last_synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String now = Instant.now().toString();
        String stableUuid = person.getStableUuid();

        if (stableUuid == null || stableUuid.isBlank()) {
            stableUuid = UUID.randomUUID().toString();
        }

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, stableUuid);
            statement.setString(2, person.getFsPid());
            statement.setString(3, person.getPreferredName());
            statement.setString(4, person.getGivenNames());
            statement.setString(5, person.getSurname());
            statement.setString(6, person.getSex().name());
            statement.setInt(7, person.isLiving() ? 1 : 0);
            statement.setString(8, person.getBirthDateText());
            statement.setString(9, person.getDeathDateText());
            statement.setString(10, person.getBirthDatePrecision().name());
            statement.setString(11, person.getDeathDatePrecision().name());
            statement.setString(12, person.getReviewedStatus().name());
            statement.setString(13, person.getNotes());
            statement.setInt(14, person.isRoot() ? 1 : 0);
            statement.setInt(15, person.isDeleted() ? 1 : 0);
            statement.setString(16, now);
            statement.setString(17, now);
            statement.setInt(18, Math.max(1, person.getVersion()));
            statement.setString(19, safeSyncStatus(person.getSyncStatus()).name());
            statement.setString(20, person.getLastSyncedAt());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    return findById(newId).orElseThrow();
                }
            }

            throw new RuntimeException("Insert succeeded but no generated key was returned.");
        } catch (SQLException ex) {
            throw new RuntimeException(buildPersonSaveErrorMessage(ex), ex);
        }
    }

    private Person update(Person person) {
        String sql = """
            UPDATE person
            SET fs_pid = ?,
                preferred_name = ?,
                given_names = ?,
                surname = ?,
                sex = ?,
                is_living = ?,
                birth_date_text = ?,
                death_date_text = ?,
                birth_date_precision = ?,
                death_date_precision = ?,
                reviewed_status = ?,
                notes = ?,
                is_root = ?,
                is_deleted = ?,
                version = version + 1,
                sync_status = 'LOCAL_ONLY',
                last_synced_at = NULL,
                updated_at = ?
            WHERE person_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, person.getFsPid());
            statement.setString(2, person.getPreferredName());
            statement.setString(3, person.getGivenNames());
            statement.setString(4, person.getSurname());
            statement.setString(5, person.getSex().name());
            statement.setInt(6, person.isLiving() ? 1 : 0);
            statement.setString(7, person.getBirthDateText());
            statement.setString(8, person.getDeathDateText());
            statement.setString(9, person.getBirthDatePrecision().name());
            statement.setString(10, person.getDeathDatePrecision().name());
            statement.setString(11, person.getReviewedStatus().name());
            statement.setString(12, person.getNotes());
            statement.setInt(13, person.isRoot() ? 1 : 0);
            statement.setInt(14, person.isDeleted() ? 1 : 0);
            statement.setString(15, Instant.now().toString());
            statement.setLong(16, person.getPersonId());

            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new RuntimeException("No person row was updated.");
            }

            return findById(person.getPersonId()).orElseThrow();
        } catch (SQLException ex) {
            throw new RuntimeException(buildPersonSaveErrorMessage(ex), ex);
        }
    }

    private String buildPersonSaveErrorMessage(SQLException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("fs_pid")) {
            return "Could not save person because that FamilySearch PID is already in use.";
        }
        return "Could not save person.";
    }

    private Person mapRow(ResultSet rs) throws SQLException {
        Person person = new Person();
        person.setPersonId(rs.getLong("person_id"));
        person.setStableUuid(rs.getString("stable_uuid"));
        person.setFsPid(rs.getString("fs_pid"));
        person.setPreferredName(rs.getString("preferred_name"));
        person.setGivenNames(rs.getString("given_names"));
        person.setSurname(rs.getString("surname"));
        person.setSex(Sex.valueOf(rs.getString("sex")));
        person.setLiving(rs.getInt("is_living") == 1);
        person.setBirthDateText(rs.getString("birth_date_text"));
        person.setDeathDateText(rs.getString("death_date_text"));
        person.setBirthDatePrecision(DatePrecision.valueOf(rs.getString("birth_date_precision")));
        person.setDeathDatePrecision(DatePrecision.valueOf(rs.getString("death_date_precision")));
        person.setReviewedStatus(ReviewedStatus.valueOf(rs.getString("reviewed_status")));
        person.setNotes(rs.getString("notes"));
        person.setRoot(rs.getInt("is_root") == 1);
        person.setDeleted(rs.getInt("is_deleted") == 1);
        person.setCreatedAt(rs.getString("created_at"));
        person.setUpdatedAt(rs.getString("updated_at"));
        person.setVersion(rs.getInt("version"));
        person.setSyncStatus(readSyncStatus(rs.getString("sync_status")));
        person.setLastSyncedAt(rs.getString("last_synced_at"));
        return person;
    }

    private SyncStatus safeSyncStatus(SyncStatus syncStatus) {
        return syncStatus == null ? SyncStatus.LOCAL_ONLY : syncStatus;
    }

    private SyncStatus readSyncStatus(String value) {
        if (value == null || value.isBlank()) {
            return SyncStatus.LOCAL_ONLY;
        }
        return SyncStatus.valueOf(value);
    }
}
