package st.consoleapp.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcModificationRepositoryTest {

    private JdbcModificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcModificationRepository(
                "jdbc:h2:mem:test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
    }

    @Test
    void shouldSaveModification() {
        repository.saveModification("cmd-data-modify-user1-001", "user1");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(1, stats.get("user1"));
    }

    @Test
    void shouldAggregateMultipleModificationsPerUser() {
        repository.saveModification("cmd-data-modify-user1-001", "user1");
        repository.saveModification("cmd-data-modify-user1-002", "user1");
        repository.saveModification("cmd-data-modify-user2-003", "user2");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(2, stats.get("user1"));
        assertEquals(1, stats.get("user2"));
    }

    @Test
    void shouldReturnEmptyWhenNoData() {
        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertTrue(stats.isEmpty());
    }
}