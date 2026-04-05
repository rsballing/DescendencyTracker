package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.LineStewardship;
import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;
import family.balling.descendencytracker.repository.LineStewardshipRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SqliteLineStewardshipRepository implements LineStewardshipRepository {
    private final DatabaseManager databaseManager;

    public SqliteLineStewardshipRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Optional<LineStewardship> findByAncestorPersonId(long ancestorPersonId) {
        String sql = """
            SELECT ancestor_person_id,
                   stewardship_status,
                   notes,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM line_stewardship
            WHERE ancestor_person_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, ancestorPersonId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load line stewardship.", ex);
        }
    }

    @Override
    public Map<Long, LineStewardship> findByAncestorPersonIds(List<Long> ancestorPersonIds) {
        List<Long> validIds = ancestorPersonIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (validIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = validIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
            SELECT ancestor_person_id,
                   stewardship_status,
                   notes,
                   updated_at,
                   version,
                   sync_status,
                   last_synced_at
            FROM line_stewardship
            WHERE ancestor_person_id IN (%s)
            """.formatted(placeholders);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < validIds.size(); i++) {
                statement.setLong(i + 1, validIds.get(i));
            }

            Map<Long, LineStewardship> results = new LinkedHashMap<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LineStewardship stewardship = mapRow(rs);
                    results.put(stewardship.getAncestorPersonId(), stewardship);
                }
            }

            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load line stewardship.", ex);
        }
    }

    @Override
    public LineStewardship save(LineStewardship stewardship) {
        String sql = """
            INSERT INTO line_stewardship (
                ancestor_person_id,
                stewardship_status,
                notes,
                updated_at,
                version,
                sync_status,
                last_synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(ancestor_person_id) DO UPDATE SET
                stewardship_status = excluded.stewardship_status,
                notes = excluded.notes,
                updated_at = excluded.updated_at,
                version = line_stewardship.version + 1,
                sync_status = 'LOCAL_ONLY',
                last_synced_at = NULL
            """;

        String now = Instant.now().toString();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, stewardship.getAncestorPersonId());
            statement.setString(2, stewardship.getStewardshipStatus().name());
            statement.setString(3, stewardship.getNotes());
            statement.setString(4, now);
            statement.setInt(5, Math.max(1, stewardship.getVersion()));
            statement.setString(6, safeSyncStatus(stewardship.getSyncStatus()).name());
            statement.setString(7, stewardship.getLastSyncedAt());

            statement.executeUpdate();

            return findByAncestorPersonId(stewardship.getAncestorPersonId()).orElseThrow();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not save line stewardship.", ex);
        }
    }

    private LineStewardship mapRow(ResultSet rs) throws SQLException {
        LineStewardship stewardship = new LineStewardship();
        stewardship.setAncestorPersonId(rs.getLong("ancestor_person_id"));
        stewardship.setStewardshipStatus(readStatus(rs.getString("stewardship_status")));
        stewardship.setNotes(rs.getString("notes"));
        stewardship.setUpdatedAt(rs.getString("updated_at"));
        stewardship.setVersion(rs.getInt("version"));
        stewardship.setSyncStatus(readSyncStatus(rs.getString("sync_status")));
        stewardship.setLastSyncedAt(rs.getString("last_synced_at"));
        return stewardship;
    }

    private LineStewardshipStatus readStatus(String value) {
        if (value == null || value.isBlank()) {
            return LineStewardshipStatus.UNASSIGNED;
        }
        return LineStewardshipStatus.valueOf(value);
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
