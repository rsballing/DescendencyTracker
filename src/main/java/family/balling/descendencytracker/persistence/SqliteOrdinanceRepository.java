package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;
import family.balling.descendencytracker.repository.OrdinanceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class SqliteOrdinanceRepository implements OrdinanceRepository {
    private final DatabaseManager databaseManager;

    public SqliteOrdinanceRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Optional<PersonOrdinanceStatus> findByPersonId(long personId) {
        String sql = """
            SELECT person_id,
                   baptism_status,
                   confirmation_status,
                   initiatory_status,
                   endowment_status,
                   sealed_to_parents_status,
                   baptism_reserved,
                   confirmation_reserved,
                   initiatory_reserved,
                   endowment_reserved,
                   sealed_to_parents_reserved,
                   ordinance_notes,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM person_ordinance_status
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
            throw new RuntimeException("Could not load ordinance status.", ex);
        }
    }

    @Override
    public PersonOrdinanceStatus save(PersonOrdinanceStatus status) {
        String sql = """
            INSERT INTO person_ordinance_status (
                person_id,
                baptism_status,
                confirmation_status,
                initiatory_status,
                endowment_status,
                sealed_to_parents_status,
                baptism_reserved,
                confirmation_reserved,
                initiatory_reserved,
                endowment_reserved,
                sealed_to_parents_reserved,
                ordinance_notes,
                updated_at,
                version,
                sync_status,
                last_synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(person_id) DO UPDATE SET
                baptism_status = excluded.baptism_status,
                confirmation_status = excluded.confirmation_status,
                initiatory_status = excluded.initiatory_status,
                endowment_status = excluded.endowment_status,
                sealed_to_parents_status = excluded.sealed_to_parents_status,
                baptism_reserved = excluded.baptism_reserved,
                confirmation_reserved = excluded.confirmation_reserved,
                initiatory_reserved = excluded.initiatory_reserved,
                endowment_reserved = excluded.endowment_reserved,
                sealed_to_parents_reserved = excluded.sealed_to_parents_reserved,
                ordinance_notes = excluded.ordinance_notes,
                updated_at = excluded.updated_at,
                version = person_ordinance_status.version + 1,
                sync_status = 'LOCAL_ONLY',
                last_synced_at = NULL
            """;

        String now = Instant.now().toString();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, status.getPersonId());
            statement.setString(2, status.getBaptismStatus().name());
            statement.setString(3, status.getConfirmationStatus().name());
            statement.setString(4, status.getInitiatoryStatus().name());
            statement.setString(5, status.getEndowmentStatus().name());
            statement.setString(6, status.getSealedToParentsStatus().name());
            statement.setInt(7, status.isBaptismReserved() ? 1 : 0);
            statement.setInt(8, status.isConfirmationReserved() ? 1 : 0);
            statement.setInt(9, status.isInitiatoryReserved() ? 1 : 0);
            statement.setInt(10, status.isEndowmentReserved() ? 1 : 0);
            statement.setInt(11, status.isSealedToParentsReserved() ? 1 : 0);
            statement.setString(12, status.getOrdinanceNotes());
            statement.setString(13, now);
            statement.setInt(14, Math.max(1, status.getVersion()));
            statement.setString(15, safeSyncStatus(status.getSyncStatus()).name());
            statement.setString(16, status.getLastSyncedAt());

            statement.executeUpdate();

            return findByPersonId(status.getPersonId()).orElseThrow();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not save ordinance status.", ex);
        }
    }

    private PersonOrdinanceStatus mapRow(ResultSet rs) throws SQLException {
        PersonOrdinanceStatus status = new PersonOrdinanceStatus();
        status.setPersonId(rs.getLong("person_id"));
        status.setBaptismStatus(readStatus(rs.getString("baptism_status")));
        status.setConfirmationStatus(readStatus(rs.getString("confirmation_status")));
        status.setInitiatoryStatus(readStatus(rs.getString("initiatory_status")));
        status.setEndowmentStatus(readStatus(rs.getString("endowment_status")));
        status.setSealedToParentsStatus(readStatus(rs.getString("sealed_to_parents_status")));
        status.setBaptismReserved(rs.getInt("baptism_reserved") == 1);
        status.setConfirmationReserved(rs.getInt("confirmation_reserved") == 1);
        status.setInitiatoryReserved(rs.getInt("initiatory_reserved") == 1);
        status.setEndowmentReserved(rs.getInt("endowment_reserved") == 1);
        status.setSealedToParentsReserved(rs.getInt("sealed_to_parents_reserved") == 1);
        status.setOrdinanceNotes(rs.getString("ordinance_notes"));
        status.setUpdatedAt(rs.getString("updated_at"));
        status.setVersion(rs.getInt("version"));
        status.setSyncStatus(readSyncStatus(rs.getString("sync_status")));
        status.setLastSyncedAt(rs.getString("last_synced_at"));
        return status;
    }

    private OrdinanceStatus readStatus(String value) {
        if (value == null || value.isBlank()) {
            return OrdinanceStatus.UNKNOWN;
        }
        return OrdinanceStatus.valueOf(value);
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
