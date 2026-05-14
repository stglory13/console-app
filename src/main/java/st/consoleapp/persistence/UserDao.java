package st.consoleapp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized boolean isLoggedIn(String userId) throws SQLException {
        String sql = "SELECT logged_in FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("logged_in");
                }
                return false; // user not exists, not logged in
            }
        }
    }

    public synchronized void login(String userId) throws SQLException {
        String updateSql = "UPDATE users SET logged_in = TRUE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, userId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                // User does not exist, insert
                String insertSql = "INSERT INTO users (id, logged_in) VALUES (?, TRUE)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, userId);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    public synchronized void logout(String userId) throws SQLException {
        if (isLoggedIn(userId)) {
            updateLoggedIn(userId, false);
        }
    }

    public synchronized int getLoggedInCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE logged_in = TRUE";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private boolean userExists(String userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertUser(String userId, boolean loggedIn) throws SQLException {
        String sql = "INSERT INTO users (id, logged_in) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setBoolean(2, loggedIn);
            stmt.executeUpdate();
        }
    }

    private void updateLoggedIn(String userId, boolean loggedIn) throws SQLException {
        String sql = "UPDATE users SET logged_in = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, loggedIn);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }
}
