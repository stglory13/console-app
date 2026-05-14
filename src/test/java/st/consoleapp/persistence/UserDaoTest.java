package st.consoleapp.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoTest {

    private DatabaseManager dbManager;
    private UserDao userDao;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DatabaseManager();
        userDao = new UserDao(dbManager.getConnection());
    }

    @AfterEach
    void tearDown() throws SQLException {
        dbManager.close();
    }

    @Test
    void shouldLoginUser() throws SQLException {
        String userId = "user1";

        assertFalse(userDao.isLoggedIn(userId));

        userDao.login(userId);
        assertTrue(userDao.isLoggedIn(userId));
    }

    @Test
    void shouldLogoutUser() throws SQLException {
        String userId = "user1";

        userDao.login(userId);
        assertTrue(userDao.isLoggedIn(userId));

        userDao.logout(userId);
        assertFalse(userDao.isLoggedIn(userId));
    }

    @Test
    void shouldNotLoginAlreadyLoggedInUser() throws SQLException {
        String userId = "user1";

        userDao.login(userId);
        assertTrue(userDao.isLoggedIn(userId));

        // Try login again, should remain logged in
        userDao.login(userId);
        assertTrue(userDao.isLoggedIn(userId));
    }

    @Test
    void shouldNotLogoutNotLoggedInUser() throws SQLException {
        String userId = "user1";

        assertFalse(userDao.isLoggedIn(userId));

        userDao.logout(userId);
        assertFalse(userDao.isLoggedIn(userId));
    }

    @Test
    void shouldGetLoggedInCount() throws SQLException {
        userDao.login("user1");
        userDao.login("user2");
        userDao.logout("user2");

        assertEquals(1, userDao.getLoggedInCount());
    }
}
