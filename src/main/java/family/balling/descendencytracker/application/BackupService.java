package family.balling.descendencytracker.application;

import family.balling.descendencytracker.persistence.DatabaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackupService {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DatabaseManager databaseManager;

    public BackupService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Path exportBackup(Path targetPath) {
        if (targetPath == null) {
            throw new IllegalArgumentException("A target backup file must be selected.");
        }

        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.deleteIfExists(targetPath);

            try (Connection connection = databaseManager.getConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("VACUUM INTO " + sqlStringLiteral(targetPath.toAbsolutePath().toString()));
            }

            return targetPath.toAbsolutePath();
        } catch (IOException | SQLException ex) {
            throw new RuntimeException("Could not export backup.", ex);
        }
    }

    public Path importBackup(Path sourcePath) {
        if (sourcePath == null) {
            throw new IllegalArgumentException("A backup file must be selected.");
        }
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("The selected backup file does not exist.");
        }

        validateBackupFile(sourcePath);

        Path safetyBackupPath = createSafetyBackup();

        boolean attached = false;

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = OFF");
            connection.setAutoCommit(false);

            try {
                statement.execute("ATTACH DATABASE " + sqlStringLiteral(sourcePath.toAbsolutePath().toString()) + " AS imported");
                attached = true;

                ensureRequiredImportedTablesExist(connection);

                statement.executeUpdate("DELETE FROM person_ordinance_status");
                statement.executeUpdate("DELETE FROM parent_child_link");
                statement.executeUpdate("DELETE FROM spouse_link");
                statement.executeUpdate("DELETE FROM person");
                statement.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('person', 'parent_child_link', 'spouse_link')");

                copySharedColumns(connection, "person");
                copySharedColumns(connection, "parent_child_link");
                copySharedColumns(connection, "spouse_link");

                if (tableExists(connection, "imported", "person_ordinance_status")) {
                    copySharedColumns(connection, "person_ordinance_status");
                }

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);

                if (attached) {
                    try {
                        statement.execute("DETACH DATABASE imported");
                    } catch (SQLException ignore) {
                    }
                }

                statement.execute("PRAGMA foreign_keys = ON");
            }

            return safetyBackupPath;
        } catch (Exception ex) {
            throw new RuntimeException("Could not import backup.", ex);
        }
    }

    private Path createSafetyBackup() {
        String fileName = "descendencytracker-pre-import-" + TIMESTAMP_FORMAT.format(LocalDateTime.now()) + ".db";
        Path safetyPath = Path.of(System.getProperty("user.home")).resolve(fileName);
        return exportBackup(safetyPath);
    }

    private void validateBackupFile(Path sourcePath) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sourcePath.toAbsolutePath())) {
            if (!tableExists(connection, "main", "person")) {
                throw new IllegalArgumentException("The selected file is not a valid DescendencyTracker backup.");
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA quick_check")) {

                if (rs.next()) {
                    String result = rs.getString(1);
                    if (result != null && !"ok".equalsIgnoreCase(result.trim())) {
                        throw new IllegalArgumentException("The selected backup file did not pass SQLite integrity checking.");
                    }
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Could not validate the selected backup file.", ex);
        }
    }

    private void ensureRequiredImportedTablesExist(Connection connection) throws SQLException {
        if (!tableExists(connection, "imported", "person")) {
            throw new IllegalArgumentException("The backup file is missing the person table.");
        }
        if (!tableExists(connection, "imported", "parent_child_link")) {
            throw new IllegalArgumentException("The backup file is missing the parent_child_link table.");
        }
        if (!tableExists(connection, "imported", "spouse_link")) {
            throw new IllegalArgumentException("The backup file is missing the spouse_link table.");
        }
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT name FROM " + schemaName + ".sqlite_master WHERE type = 'table' AND name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void copySharedColumns(Connection connection, String tableName) throws SQLException {
        List<String> mainColumns = getColumnNames(connection, "main", tableName);
        Set<String> importedColumns = new HashSet<>(getColumnNames(connection, "imported", tableName));
        List<String> sharedColumns = new ArrayList<>();

        for (String column : mainColumns) {
            if (importedColumns.contains(column)) {
                sharedColumns.add(column);
            }
        }

        if (sharedColumns.isEmpty()) {
            throw new IllegalArgumentException("The backup file does not contain compatible columns for table " + tableName + ".");
        }

        String joinedColumns = String.join(", ", sharedColumns);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO " + tableName + " (" + joinedColumns + ") SELECT " + joinedColumns + " FROM imported." + tableName
            );
        }
    }

    private List<String> getColumnNames(Connection connection, String schemaName, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA " + schemaName + ".table_info(" + tableName + ")")) {

            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        return columns;
    }

    private String sqlStringLiteral(String raw) {
        return "'" + raw.replace("'", "''") + "'";
    }
}
