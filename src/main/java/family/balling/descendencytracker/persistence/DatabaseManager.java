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
        try {
            Path appDirectory = Paths.get(System.getProperty("user.home"), ".descendency-tracker");
            Files.createDirectories(appDirectory);
            this.databasePath = appDirectory.resolve("descendency-tracker.db");
            this.jdbcUrl = "jdbc:sqlite:" + databasePath;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the application data directory.", ex);
        }
    }

    public void initialize() {
        try (Connection connection = getConnection()) {
            SchemaMigrator.migrate(connection);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize the database.", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public Path getDatabasePath() {
        return databasePath;
    }
}