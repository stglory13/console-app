package st.consoleapp.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ModificationDao {

    private final Connection connection;

    public ModificationDao(Connection connection) {
        this.connection = connection;
    }

    public synchronized void addModification(String userId) throws SQLException {
        String sql = "INSERT INTO modifications (user_id) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    public synchronized Map<String, Integer> getModificationCounts() throws SQLException {
        String sql = "SELECT user_id, COUNT(*) as count FROM modifications GROUP BY user_id";
        Map<String, Integer> counts = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("user_id"), rs.getInt("count"));
            }
        }
        return counts;
    }
}
