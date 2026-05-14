package st.consoleapp.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private final Connection connection;

    public DatabaseManager() throws SQLException {
        this.connection = DriverManager.getConnection(URL);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id VARCHAR PRIMARY KEY, logged_in BOOLEAN DEFAULT FALSE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS modifications (id INT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            // Clear tables for clean test state
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("TRUNCATE TABLE modifications");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        connection.close();
    }
}
