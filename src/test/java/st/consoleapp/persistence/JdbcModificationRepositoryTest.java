package st.consoleapp.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdbcModificationRepositoryTest {

    private JdbcModificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcModificationRepository("jdbc:h2:mem:test-" + System.nanoTime());
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void shouldSaveSingleModification() {
        repository.saveModification("cmd-data-modify-user1-001", "user1");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(1, stats.get("user1"));
    }

    @Test
    void shouldSaveMultipleModificationsForDifferentUsers() {
        repository.saveModification("cmd-data-modify-user1-001", "user1");
        repository.saveModification("cmd-data-modify-user2-001", "user2");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(1, stats.get("user1"));
        assertEquals(1, stats.get("user2"));
    }

    @Test
    void shouldStoreMultipleEntriesForSameUser() {
        repository.saveModification("cmd-data-modify-user1-001", "user1");
        repository.saveModification("cmd-data-modify-user1-002", "user1");
        repository.saveModification("cmd-data-modify-user1-003", "user1");

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(3, stats.get("user1"));
    }

    @Test
    void shouldReturnEmptyStatsWhenNoData() {
        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertTrue(stats.isEmpty());
    }
}