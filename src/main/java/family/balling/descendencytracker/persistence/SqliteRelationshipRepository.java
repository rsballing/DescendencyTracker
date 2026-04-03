package family.balling.descendencytracker.persistence;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.repository.RelationshipRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqliteRelationshipRepository implements RelationshipRepository {
    private final DatabaseManager databaseManager;

    public SqliteRelationshipRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<ParentChildLink> findParentsForChild(long childPersonId) {
        String sql = """
            SELECT pcl.link_id,
                   pcl.stable_uuid,
                   pcl.parent_person_id,
                   pcl.child_person_id,
                   pcl.child_order,
                   pcl.notes,
                   pcl.is_deleted,
                   pcl.created_at,
                   pcl.updated_at,
                   p.preferred_name AS parent_preferred_name,
                   p.given_names AS parent_given_names,
                   p.surname AS parent_surname,
                   c.preferred_name AS child_preferred_name,
                   c.given_names AS child_given_names,
                   c.surname AS child_surname
            FROM parent_child_link pcl
            JOIN person p ON p.person_id = pcl.parent_person_id
            JOIN person c ON c.person_id = pcl.child_person_id
            WHERE pcl.child_person_id = ?
              AND pcl.is_deleted = 0
              AND p.is_deleted = 0
              AND c.is_deleted = 0
            ORDER BY COALESCE(p.surname, '') COLLATE NOCASE,
                     COALESCE(p.given_names, '') COLLATE NOCASE,
                     COALESCE(p.preferred_name, '') COLLATE NOCASE
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, childPersonId);

            try (ResultSet rs = statement.executeQuery()) {
                List<ParentChildLink> links = new ArrayList<>();
                while (rs.next()) {
                    links.add(mapParentChildRow(rs));
                }
                return links;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load parents.", ex);
        }
    }

    @Override
    public List<ParentChildLink> findChildrenForParent(long parentPersonId) {
        String sql = """
            SELECT pcl.link_id,
                   pcl.stable_uuid,
                   pcl.parent_person_id,
                   pcl.child_person_id,
                   pcl.child_order,
                   pcl.notes,
                   pcl.is_deleted,
                   pcl.created_at,
                   pcl.updated_at,
                   p.preferred_name AS parent_preferred_name,
                   p.given_names AS parent_given_names,
                   p.surname AS parent_surname,
                   c.preferred_name AS child_preferred_name,
                   c.given_names AS child_given_names,
                   c.surname AS child_surname
            FROM parent_child_link pcl
            JOIN person p ON p.person_id = pcl.parent_person_id
            JOIN person c ON c.person_id = pcl.child_person_id
            WHERE pcl.parent_person_id = ?
              AND pcl.is_deleted = 0
              AND p.is_deleted = 0
              AND c.is_deleted = 0
            ORDER BY CASE WHEN pcl.child_order IS NULL THEN 1 ELSE 0 END,
                     pcl.child_order,
                     COALESCE(c.surname, '') COLLATE NOCASE,
                     COALESCE(c.given_names, '') COLLATE NOCASE,
                     COALESCE(c.preferred_name, '') COLLATE NOCASE
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, parentPersonId);

            try (ResultSet rs = statement.executeQuery()) {
                List<ParentChildLink> links = new ArrayList<>();
                while (rs.next()) {
                    links.add(mapParentChildRow(rs));
                }
                return links;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load children.", ex);
        }
    }

    @Override
    public List<SpouseLink> findSpousesForPerson(long personId) {
        String sql = """
            SELECT sl.spouse_link_id,
                   sl.stable_uuid,
                   sl.person_a_id,
                   sl.person_b_id,
                   sl.marriage_date_text,
                   sl.marriage_notes,
                   sl.sealing_to_spouse_status,
                   sl.sealing_status_date,
                   sl.sealing_notes,
                   sl.is_deleted,
                   sl.created_at,
                   sl.updated_at,
                   a.preferred_name AS person_a_preferred_name,
                   a.given_names AS person_a_given_names,
                   a.surname AS person_a_surname,
                   b.preferred_name AS person_b_preferred_name,
                   b.given_names AS person_b_given_names,
                   b.surname AS person_b_surname
            FROM spouse_link sl
            JOIN person a ON a.person_id = sl.person_a_id
            JOIN person b ON b.person_id = sl.person_b_id
            WHERE (sl.person_a_id = ? OR sl.person_b_id = ?)
              AND sl.is_deleted = 0
              AND a.is_deleted = 0
              AND b.is_deleted = 0
            ORDER BY COALESCE(b.surname, a.surname, '') COLLATE NOCASE,
                     COALESCE(b.given_names, a.given_names, '') COLLATE NOCASE
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, personId);
            statement.setLong(2, personId);

            try (ResultSet rs = statement.executeQuery()) {
                List<SpouseLink> links = new ArrayList<>();
                while (rs.next()) {
                    links.add(mapSpouseRow(rs));
                }
                return links;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load spouses.", ex);
        }
    }

    @Override
    public ParentChildLink addParentChild(long parentPersonId, long childPersonId, Integer childOrder, String notes) {
        String sql = """
            INSERT INTO parent_child_link (
                stable_uuid,
                parent_person_id,
                child_person_id,
                child_order,
                notes,
                is_deleted,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, 0, ?, ?)
            """;

        String now = Instant.now().toString();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, parentPersonId);
            statement.setLong(3, childPersonId);

            if (childOrder == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, childOrder);
            }

            statement.setString(5, notes);
            statement.setString(6, now);
            statement.setString(7, now);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findParentChildById(keys.getLong(1));
                }
            }

            throw new RuntimeException("Insert succeeded but no key was returned.");
        } catch (SQLException ex) {
            throw new RuntimeException("Could not add parent-child link.", ex);
        }
    }

    @Override
    public ParentChildLink updateParentChild(long linkId, long parentPersonId, long childPersonId, Integer childOrder, String notes) {
        String sql = """
            UPDATE parent_child_link
            SET parent_person_id = ?,
                child_person_id = ?,
                child_order = ?,
                notes = ?,
                updated_at = ?
            WHERE link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, parentPersonId);
            statement.setLong(2, childPersonId);

            if (childOrder == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, childOrder);
            }

            statement.setString(4, notes);
            statement.setString(5, Instant.now().toString());
            statement.setLong(6, linkId);

            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new RuntimeException("No parent-child link row was updated.");
            }

            return findParentChildById(linkId);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not update parent-child link.", ex);
        }
    }

    @Override
    public void softDeleteParentChild(long linkId) {
        String sql = """
            UPDATE parent_child_link
            SET is_deleted = 1,
                updated_at = ?
            WHERE link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, Instant.now().toString());
            statement.setLong(2, linkId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not delete parent-child link.", ex);
        }
    }

    @Override
    public SpouseLink addSpouse(
            long personAId,
            long personBId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    ) {
        long normalizedA = Math.min(personAId, personBId);
        long normalizedB = Math.max(personAId, personBId);

        String sql = """
            INSERT INTO spouse_link (
                stable_uuid,
                person_a_id,
                person_b_id,
                marriage_date_text,
                marriage_notes,
                sealing_to_spouse_status,
                sealing_status_date,
                sealing_notes,
                is_deleted,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
            """;

        String now = Instant.now().toString();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, normalizedA);
            statement.setLong(3, normalizedB);
            statement.setString(4, marriageDateText);
            statement.setString(5, marriageNotes);
            statement.setString(6, safeStatus(sealingToSpouseStatus).name());
            statement.setString(7, sealingStatusDate);
            statement.setString(8, sealingNotes);
            statement.setString(9, now);
            statement.setString(10, now);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findSpouseById(keys.getLong(1));
                }
            }

            throw new RuntimeException("Insert succeeded but no key was returned.");
        } catch (SQLException ex) {
            throw new RuntimeException("Could not add spouse link.", ex);
        }
    }

    @Override
    public SpouseLink updateSpouse(
            long spouseLinkId,
            long personAId,
            long personBId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    ) {
        long normalizedA = Math.min(personAId, personBId);
        long normalizedB = Math.max(personAId, personBId);

        String sql = """
            UPDATE spouse_link
            SET person_a_id = ?,
                person_b_id = ?,
                marriage_date_text = ?,
                marriage_notes = ?,
                sealing_to_spouse_status = ?,
                sealing_status_date = ?,
                sealing_notes = ?,
                updated_at = ?
            WHERE spouse_link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, normalizedA);
            statement.setLong(2, normalizedB);
            statement.setString(3, marriageDateText);
            statement.setString(4, marriageNotes);
            statement.setString(5, safeStatus(sealingToSpouseStatus).name());
            statement.setString(6, sealingStatusDate);
            statement.setString(7, sealingNotes);
            statement.setString(8, Instant.now().toString());
            statement.setLong(9, spouseLinkId);

            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new RuntimeException("No spouse link row was updated.");
            }

            return findSpouseById(spouseLinkId);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not update spouse link.", ex);
        }
    }

    @Override
    public void softDeleteSpouse(long spouseLinkId) {
        String sql = """
            UPDATE spouse_link
            SET is_deleted = 1,
                updated_at = ?
            WHERE spouse_link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, Instant.now().toString());
            statement.setLong(2, spouseLinkId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not delete spouse link.", ex);
        }
    }

    private ParentChildLink findParentChildById(long linkId) {
        String sql = """
            SELECT pcl.link_id,
                   pcl.stable_uuid,
                   pcl.parent_person_id,
                   pcl.child_person_id,
                   pcl.child_order,
                   pcl.notes,
                   pcl.is_deleted,
                   pcl.created_at,
                   pcl.updated_at,
                   p.preferred_name AS parent_preferred_name,
                   p.given_names AS parent_given_names,
                   p.surname AS parent_surname,
                   c.preferred_name AS child_preferred_name,
                   c.given_names AS child_given_names,
                   c.surname AS child_surname
            FROM parent_child_link pcl
            JOIN person p ON p.person_id = pcl.parent_person_id
            JOIN person c ON c.person_id = pcl.child_person_id
            WHERE pcl.link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, linkId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapParentChildRow(rs);
                }
            }

            throw new RuntimeException("Parent-child link not found.");
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load parent-child link.", ex);
        }
    }

    private SpouseLink findSpouseById(long spouseLinkId) {
        String sql = """
            SELECT sl.spouse_link_id,
                   sl.stable_uuid,
                   sl.person_a_id,
                   sl.person_b_id,
                   sl.marriage_date_text,
                   sl.marriage_notes,
                   sl.sealing_to_spouse_status,
                   sl.sealing_status_date,
                   sl.sealing_notes,
                   sl.is_deleted,
                   sl.created_at,
                   sl.updated_at,
                   a.preferred_name AS person_a_preferred_name,
                   a.given_names AS person_a_given_names,
                   a.surname AS person_a_surname,
                   b.preferred_name AS person_b_preferred_name,
                   b.given_names AS person_b_given_names,
                   b.surname AS person_b_surname
            FROM spouse_link sl
            JOIN person a ON a.person_id = sl.person_a_id
            JOIN person b ON b.person_id = sl.person_b_id
            WHERE sl.spouse_link_id = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, spouseLinkId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapSpouseRow(rs);
                }
            }

            throw new RuntimeException("Spouse link not found.");
        } catch (SQLException ex) {
            throw new RuntimeException("Could not load spouse link.", ex);
        }
    }

    private ParentChildLink mapParentChildRow(ResultSet rs) throws SQLException {
        ParentChildLink link = new ParentChildLink();
        link.setLinkId(rs.getLong("link_id"));
        link.setStableUuid(rs.getString("stable_uuid"));
        link.setParentPersonId(rs.getLong("parent_person_id"));
        link.setChildPersonId(rs.getLong("child_person_id"));

        int childOrder = rs.getInt("child_order");
        if (rs.wasNull()) {
            link.setChildOrder(null);
        } else {
            link.setChildOrder(childOrder);
        }

        link.setNotes(rs.getString("notes"));
        link.setDeleted(rs.getInt("is_deleted") == 1);
        link.setCreatedAt(rs.getString("created_at"));
        link.setUpdatedAt(rs.getString("updated_at"));
        link.setParentDisplayName(buildDisplayName(
                rs.getString("parent_preferred_name"),
                rs.getString("parent_given_names"),
                rs.getString("parent_surname")
        ));
        link.setChildDisplayName(buildDisplayName(
                rs.getString("child_preferred_name"),
                rs.getString("child_given_names"),
                rs.getString("child_surname")
        ));
        return link;
    }

    private SpouseLink mapSpouseRow(ResultSet rs) throws SQLException {
        SpouseLink link = new SpouseLink();
        link.setSpouseLinkId(rs.getLong("spouse_link_id"));
        link.setStableUuid(rs.getString("stable_uuid"));
        link.setPersonAId(rs.getLong("person_a_id"));
        link.setPersonBId(rs.getLong("person_b_id"));
        link.setMarriageDateText(rs.getString("marriage_date_text"));
        link.setMarriageNotes(rs.getString("marriage_notes"));
        link.setSealingToSpouseStatus(readStatus(rs.getString("sealing_to_spouse_status")));
        link.setSealingStatusDate(rs.getString("sealing_status_date"));
        link.setSealingNotes(rs.getString("sealing_notes"));
        link.setDeleted(rs.getInt("is_deleted") == 1);
        link.setCreatedAt(rs.getString("created_at"));
        link.setUpdatedAt(rs.getString("updated_at"));
        link.setPersonADisplayName(buildDisplayName(
                rs.getString("person_a_preferred_name"),
                rs.getString("person_a_given_names"),
                rs.getString("person_a_surname")
        ));
        link.setPersonBDisplayName(buildDisplayName(
                rs.getString("person_b_preferred_name"),
                rs.getString("person_b_given_names"),
                rs.getString("person_b_surname")
        ));
        return link;
    }

    private OrdinanceStatus safeStatus(OrdinanceStatus status) {
        return status == null ? OrdinanceStatus.UNKNOWN : status;
    }

    private OrdinanceStatus readStatus(String value) {
        if (value == null || value.isBlank()) {
            return OrdinanceStatus.UNKNOWN;
        }
        return OrdinanceStatus.valueOf(value);
    }

    private String buildDisplayName(String preferredName, String givenNames, String surname) {
        if (preferredName != null && !preferredName.isBlank()) {
            return preferredName;
        }

        StringBuilder builder = new StringBuilder();

        if (givenNames != null && !givenNames.isBlank()) {
            builder.append(givenNames.trim());
        }

        if (surname != null && !surname.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(surname.trim());
        }

        if (builder.length() > 0) {
            return builder.toString();
        }

        return "(Unnamed Person)";
    }
}