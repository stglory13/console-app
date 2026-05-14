package st.consoleapp.processing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.consoleapp.command.Command;
import st.consoleapp.command.CommandType;
import st.consoleapp.persistence.DatabaseManager;
import st.consoleapp.persistence.ModificationDao;
import st.consoleapp.persistence.UserDao;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandProcessorTest {

    private DatabaseManager dbManager;
    private UserDao userDao;
    private ModificationDao modificationDao;
    private CommandProcessor processor;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DatabaseManager();
        userDao = new UserDao(dbManager.getConnection());
        modificationDao = new ModificationDao(dbManager.getConnection());
        processor = new CommandProcessor(userDao, modificationDao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        dbManager.close();
    }

    @Test
    void shouldProcessLoginCommand() throws SQLException {
        Command command = new Command(CommandType.LOGIN, "user1", "LOGIN(user1)");

        processor.process(command);

        assertTrue(userDao.isLoggedIn("user1"));
    }

    @Test
    void shouldProcessLogoutCommand() throws SQLException {
        userDao.login("user1");
        Command command = new Command(CommandType.LOGOUT, "user1", "LOGOUT(user1)");

        processor.process(command);

        assertFalse(userDao.isLoggedIn("user1"));
    }

    @Test
    void shouldProcessDataModifyCommandForLoggedInUser() throws SQLException {
        userDao.login("user1");
        Command command = new Command(CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)");

        processor.process(command);

        var counts = modificationDao.getModificationCounts();
        assertEquals(1, counts.get("user1"));
    }

    @Test
    void shouldNotProcessDataModifyCommandForNotLoggedInUser() throws SQLException {
        Command command = new Command(CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)");

        processor.process(command);

        var counts = modificationDao.getModificationCounts();
        assertTrue(counts.isEmpty());
    }

    @Test
    void shouldProcessStatsCommand() throws SQLException {
        userDao.login("user1");
        userDao.login("user2");
        modificationDao.addModification("user1");
        modificationDao.addModification("user1");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream originalOut = System.out;
        System.setOut(printStream);

        try {
            Command command = new Command(CommandType.STATS, null, "STATS()");
            processor.process(command);

            String output = outputStream.toString();
            assertTrue(output.contains("Number of currently logged-in users: 2"));
            assertTrue(output.contains("user1: 2"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void shouldHandleConcurrentCommands() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            String userId = "user" + i;
            executor.submit(() -> {
                try {
                    processor.process(new Command(CommandType.LOGIN, userId, "LOGIN(" + userId + ")"));
                    processor.process(new Command(CommandType.DATA_MODIFY, userId, "DATA_MODIFY(" + userId + ")"));
                    processor.process(new Command(CommandType.LOGOUT, userId, "LOGOUT(" + userId + ")"));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Check that all users are logged out and have modifications
        for (int i = 0; i < numThreads; i++) {
            String userId = "user" + i;
            try {
                assertFalse(userDao.isLoggedIn(userId));
                var counts = modificationDao.getModificationCounts();
                assertEquals(1, counts.get(userId));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
