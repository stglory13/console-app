package st.consoleapp.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcModificationRepositoryTest {

    private Connection connection;
    private JdbcModificationRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        connection.createStatement().execute("""
            CREATE TABLE modifications (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                command_id VARCHAR(100),
                user_id VARCHAR(100),
                created_at TIMESTAMP
            )
        """);

        repository = new JdbcModificationRepository(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.createStatement().execute("DROP TABLE modifications");
        connection.close();
    }

    @Test
    void shouldSaveModification() throws Exception {
        repository.saveModification("cmd-1", "user1");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(1, stats.get("user1"));
    }

    @Test
    void shouldAggregateMultipleModificationsPerUser() throws Exception {
        repository.saveModification("cmd-1", "user1");
        repository.saveModification("cmd-2", "user1");
        repository.saveModification("cmd-3", "user2");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(2, stats.get("user1"));
        assertEquals(1, stats.get("user2"));
    }

    @Test
    void shouldReturnEmptyWhenNoData() throws Exception {
        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertTrue(stats.isEmpty());
    }
}