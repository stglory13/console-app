package st.consoleapp;

import org.junit.jupiter.api.Test;
import st.consoleapp.command.CommandParser;
import st.consoleapp.command.CommandType;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.JdbcModificationRepository;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.processing.CommandProcessor;
import st.consoleapp.producer.ConsoleCommandProducer;
import st.consoleapp.queue.CommandQueue;
import st.consoleapp.state.UserSessionState;
import st.consoleapp.worker.CommandWorker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleAppIT {

    /**
     * Integration test covering full application flow:
     * - multiple users
     * - login/logout scenarios (including duplicates)
     * - data modifications (valid and ignored)
     * - invalid commands handling
     * - correlation IDs (ACCEPTED / COMPLETED)
     * - ordered asynchronous processing
     * - STATS output verification
     * - graceful shutdown via EXIT
     */
    @Test
    void shouldProcessFullApplicationFlow() throws Exception {
        InputStream originalIn = System.in;

        String input = String.join(System.lineSeparator(),
                "LOGOUT(user1)",
                "LOGIN(user1)",
                "LOGIN(user2)",
                "LOGIN(user1)",

                "LOGIN()",
                "LOGIN",
                "DATA_MODIFY()",
                "HELLO(user1)",
                "INVALID_COMMAND()",

                "DATA_MODIFY(user1)",
                "DATA_MODIFY(user1)",
                "DATA_MODIFY(user2)",
                "DATA_MODIFY(user3)",

                "LOGOUT(user2)",
                "LOGOUT(user2)",
                "DATA_MODIFY(user2)",

                "LOGIN(user3)",
                "DATA_MODIFY(user3)",

                "LOGIN(user4)",
                "DATA_MODIFY(user4)",
                "DATA_MODIFY(user4)",
                "LOGOUT(user4)",
                "DATA_MODIFY(user4)",

                "LOGIN(user5)",
                "LOGOUT(user5)",
                "DATA_MODIFY(user5)",

                "LOGIN(user99)",
                "DATA_MODIFY(user99)",
                "DATA_MODIFY(user99)",
                "DATA_MODIFY(user99)",

                "STATS()",

                "LOGOUT(user1)",
                "LOGOUT(user1)",
                "LOGOUT(user3)",
                "LOGOUT(user99)",

                "EXIT()"
        ) + System.lineSeparator();

        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        CommandQueue queue = new CommandQueue();
        UserSessionState sessionState = new UserSessionState();
        TestOutputWriter output = new TestOutputWriter();

        try (ModificationRepository repository =
                     new JdbcModificationRepository("jdbc:h2:mem:console-app-it-" + System.nanoTime())) {

            CommandProcessor processor = new CommandProcessor(sessionState, repository, output);
            CommandWorker worker = new CommandWorker(queue, processor);
            Thread workerThread = new Thread(worker, "test-command-worker");

            workerThread.start();

            ConsoleCommandProducer producer =
                    new ConsoleCommandProducer(new CommandParser(), queue, output);

            producer.start();

            workerThread.join(3000);

            assertFalse(workerThread.isAlive());

            assertFalse(sessionState.isLoggedIn("user1"));
            assertFalse(sessionState.isLoggedIn("user2"));
            assertFalse(sessionState.isLoggedIn("user3"));
            assertFalse(sessionState.isLoggedIn("user4"));
            assertFalse(sessionState.isLoggedIn("user5"));
            assertFalse(sessionState.isLoggedIn("user99"));

            Map<String, Integer> stats = repository.countModificationsPerUser();

            assertEquals(2, stats.get("user1"));
            assertEquals(1, stats.get("user2"));
            assertEquals(1, stats.get("user3"));
            assertEquals(2, stats.get("user4"));
            assertFalse(stats.containsKey("user5"));
            assertEquals(3, stats.get("user99"));

            assertAccepted("cmd-logout-user1-001", CommandType.LOGOUT, output);
            assertAccepted("cmd-login-user1-002", CommandType.LOGIN, output);
            assertAccepted("cmd-login-user2-003", CommandType.LOGIN, output);
            assertAccepted("cmd-login-user1-004", CommandType.LOGIN, output);

            assertAccepted("cmd-invalid-005", CommandType.INVALID, output);
            assertAccepted("cmd-invalid-006", CommandType.INVALID, output);
            assertAccepted("cmd-invalid-007", CommandType.INVALID, output);
            assertAccepted("cmd-invalid-008", CommandType.INVALID, output);
            assertAccepted("cmd-invalid-009", CommandType.INVALID, output);

            assertAccepted("cmd-data-modify-user1-010", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user1-011", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user2-012", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user3-013", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-logout-user2-014", CommandType.LOGOUT, output);
            assertAccepted("cmd-logout-user2-015", CommandType.LOGOUT, output);
            assertAccepted("cmd-data-modify-user2-016", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-login-user3-017", CommandType.LOGIN, output);
            assertAccepted("cmd-data-modify-user3-018", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-login-user4-019", CommandType.LOGIN, output);
            assertAccepted("cmd-data-modify-user4-020", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user4-021", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-logout-user4-022", CommandType.LOGOUT, output);
            assertAccepted("cmd-data-modify-user4-023", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-login-user5-024", CommandType.LOGIN, output);
            assertAccepted("cmd-logout-user5-025", CommandType.LOGOUT, output);
            assertAccepted("cmd-data-modify-user5-026", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-login-user99-027", CommandType.LOGIN, output);
            assertAccepted("cmd-data-modify-user99-028", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user99-029", CommandType.DATA_MODIFY, output);
            assertAccepted("cmd-data-modify-user99-030", CommandType.DATA_MODIFY, output);

            assertAccepted("cmd-stats-031", CommandType.STATS, output);

            assertAccepted("cmd-logout-user1-032", CommandType.LOGOUT, output);
            assertAccepted("cmd-logout-user1-033", CommandType.LOGOUT, output);
            assertAccepted("cmd-logout-user3-034", CommandType.LOGOUT, output);
            assertAccepted("cmd-logout-user99-035", CommandType.LOGOUT, output);

            assertAccepted("cmd-exit-036", CommandType.EXIT, output);

            assertCompletedInOrder(output,
                    expected("cmd-logout-user1-001", CommandType.LOGOUT),
                    expected("cmd-login-user1-002", CommandType.LOGIN),
                    expected("cmd-login-user2-003", CommandType.LOGIN),
                    expected("cmd-login-user1-004", CommandType.LOGIN),
                    expected("cmd-invalid-005", CommandType.INVALID),
                    expected("cmd-invalid-006", CommandType.INVALID),
                    expected("cmd-invalid-007", CommandType.INVALID),
                    expected("cmd-invalid-008", CommandType.INVALID),
                    expected("cmd-invalid-009", CommandType.INVALID),
                    expected("cmd-data-modify-user1-010", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user1-011", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user2-012", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user3-013", CommandType.DATA_MODIFY),
                    expected("cmd-logout-user2-014", CommandType.LOGOUT),
                    expected("cmd-logout-user2-015", CommandType.LOGOUT),
                    expected("cmd-data-modify-user2-016", CommandType.DATA_MODIFY),
                    expected("cmd-login-user3-017", CommandType.LOGIN),
                    expected("cmd-data-modify-user3-018", CommandType.DATA_MODIFY),
                    expected("cmd-login-user4-019", CommandType.LOGIN),
                    expected("cmd-data-modify-user4-020", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user4-021", CommandType.DATA_MODIFY),
                    expected("cmd-logout-user4-022", CommandType.LOGOUT),
                    expected("cmd-data-modify-user4-023", CommandType.DATA_MODIFY),
                    expected("cmd-login-user5-024", CommandType.LOGIN),
                    expected("cmd-logout-user5-025", CommandType.LOGOUT),
                    expected("cmd-data-modify-user5-026", CommandType.DATA_MODIFY),
                    expected("cmd-login-user99-027", CommandType.LOGIN),
                    expected("cmd-data-modify-user99-028", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user99-029", CommandType.DATA_MODIFY),
                    expected("cmd-data-modify-user99-030", CommandType.DATA_MODIFY),
                    expected("cmd-stats-031", CommandType.STATS),
                    expected("cmd-logout-user1-032", CommandType.LOGOUT),
                    expected("cmd-logout-user1-033", CommandType.LOGOUT),
                    expected("cmd-logout-user3-034", CommandType.LOGOUT),
                    expected("cmd-logout-user99-035", CommandType.LOGOUT),
                    expected("cmd-exit-036", CommandType.EXIT)
            );

            assertTrue(output.messages.stream()
                    .anyMatch(m -> m.contains("cmd=LOGIN") && m.contains("user already logged in: user1")));

            assertTrue(output.messages.stream()
                    .anyMatch(m -> m.contains("cmd=LOGOUT") && m.contains("user not logged in")));

            assertTrue(output.messages.stream()
                    .anyMatch(m -> m.contains("cmd=DATA_MODIFY")
                            && m.contains("ignored, user not logged in: user3")));

            assertTrue(output.messages.stream()
                    .anyMatch(m -> m.contains("cmd=DATA_MODIFY")
                            && m.contains("ignored, user not logged in: user2")));

            long invalidCount = output.messages.stream()
                    .filter(m -> m.contains("cmd=INVALID")
                            && m.contains("command ignored"))
                    .count();

            assertEquals(5, invalidCount);

            assertTrue(output.messages.stream()
                    .anyMatch(m -> m.contains("COMPLETED")
                            && m.contains("id=cmd-exit-036")
                            && m.contains("cmd=EXIT")));

        } finally {
            System.setIn(originalIn);
        }
    }

    private static void assertAccepted(String commandId, CommandType type, TestOutputWriter output) {
        assertTrue(
                output.messages.stream().anyMatch(m ->
                        m.contains("ACCEPTED")
                                && m.contains("id=" + commandId)
                                && m.contains("cmd=" + type)
                ),
                "Missing accepted message for " + commandId
        );
    }

    private static void assertCompletedInOrder(TestOutputWriter output, ExpectedCommand... expectedCommands) {
        int lastIndex = -1;

        for (ExpectedCommand expectedCommand : expectedCommands) {
            int index = indexOfCompletedCommand(
                    output.messages,
                    expectedCommand.commandId(),
                    expectedCommand.type()
            );

            assertTrue(
                    index > lastIndex,
                    "Command was not completed in expected order: " + expectedCommand.commandId()
            );

            lastIndex = index;
        }
    }

    private static int indexOfCompletedCommand(List<String> messages, String commandId, CommandType type) {
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);

            if (message.contains("COMPLETED")
                    && message.contains("id=" + commandId)
                    && message.contains("cmd=" + type)) {
                return i;
            }
        }

        fail("Missing completed message for " + commandId);
        return -1;
    }

    private static ExpectedCommand expected(String commandId, CommandType type) {
        return new ExpectedCommand(commandId, type);
    }

    private record ExpectedCommand(String commandId, CommandType type) {
    }

    private static class TestOutputWriter implements OutputWriter {
        private final List<String> messages = new ArrayList<>();

        @Override
        public synchronized void write(String message) {
            messages.add(message);
        }
    }
}