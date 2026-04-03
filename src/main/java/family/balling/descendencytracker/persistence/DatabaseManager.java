package family.balling.descendencytracker.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final Path databasePath;
    private final String jdbcUrl;

    public DatabaseManager() {
        this(defaultDatabasePath());
    }

    public DatabaseManager(Path databasePath) {
        try {
            Path absolutePath = databasePath.toAbsolutePath();
            if (absolutePath.getParent() != null) {
                Files.createDirectories(absolutePath.getParent());
            }
            this.databasePath = absolutePath;
            this.jdbcUrl = "jdbc:sqlite:" + databasePath;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the application data directory.", ex);
        }
    }

    private static Path defaultDatabasePath() {
        return Paths.get(System.getProperty("user.home"), ".descendency-tracker", "descendency-tracker.db");
    }

    public void initialize() {
        try (Connection connection = getConnection()) {
            SchemaMigrator.migrate(connection);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize the database.", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void ensureDriverLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Could not load the SQLite JDBC driver.", ex);
        }
    }
}
