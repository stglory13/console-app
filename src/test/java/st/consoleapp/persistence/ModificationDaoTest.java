package st.consoleapp.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModificationDaoTest {

    private DatabaseManager dbManager;
    private ModificationDao modificationDao;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DatabaseManager();
        modificationDao = new ModificationDao(dbManager.getConnection());
    }

    @AfterEach
    void tearDown() throws SQLException {
        dbManager.close();
    }

    @Test
    void shouldAddModification() throws SQLException {
        String userId = "user1";

        modificationDao.addModification(userId);

        Map<String, Integer> counts = modificationDao.getModificationCounts();
        assertEquals(1, counts.get(userId));
    }

    @Test
    void shouldAddMultipleModificationsForSameUser() throws SQLException {
        String userId = "user1";

        modificationDao.addModification(userId);
        modificationDao.addModification(userId);
        modificationDao.addModification(userId);

        Map<String, Integer> counts = modificationDao.getModificationCounts();
        assertEquals(3, counts.get(userId));
    }

    @Test
    void shouldAddModificationsForDifferentUsers() throws SQLException {
        modificationDao.addModification("user1");
        modificationDao.addModification("user2");
        modificationDao.addModification("user1");

        Map<String, Integer> counts = modificationDao.getModificationCounts();
        assertEquals(2, counts.get("user1"));
        assertEquals(1, counts.get("user2"));
    }

    @Test
    void shouldReturnEmptyCountsWhenNoModifications() throws SQLException {
        Map<String, Integer> counts = modificationDao.getModificationCounts();
        assertEquals(0, counts.size());
    }
}
