package st.consoleapp;

import org.junit.jupiter.api.Test;
import st.consoleapp.command.Command;
import st.consoleapp.command.CommandType;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.JdbcModificationRepository;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.processing.CommandProcessor;
import st.consoleapp.queue.CommandQueue;
import st.consoleapp.state.UserSessionState;
import st.consoleapp.worker.CommandWorker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAppIT {

    @Test
    void shouldProcessMultipleCommandsAndShutdownGracefully() throws Exception {
        CommandQueue queue = new CommandQueue();
        UserSessionState sessionState = new UserSessionState();
        TestOutputWriter output = new TestOutputWriter();

        try (ModificationRepository repository =
                     new JdbcModificationRepository("jdbc:h2:mem:integration-" + System.nanoTime())) {

            CommandProcessor processor = new CommandProcessor(sessionState, repository, output);
            CommandWorker worker = new CommandWorker(queue, processor);
            Thread workerThread = new Thread(worker, "test-command-worker");

            workerThread.start();

            queue.submit(command("cmd-login-user1-001", CommandType.LOGIN, "user1", "LOGIN(user1)"));
            queue.submit(command("cmd-data-modify-user1-002", CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)"));
            queue.submit(command("cmd-data-modify-user1-003", CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)"));
            queue.submit(command("cmd-data-modify-user2-004", CommandType.DATA_MODIFY, "user2", "DATA_MODIFY(user2)"));
            queue.submit(command("cmd-stats-005", CommandType.STATS, null, "STATS()"));
            queue.submit(command("cmd-logout-user1-006", CommandType.LOGOUT, "user1", "LOGOUT(user1)"));
            queue.submit(command("cmd-exit-007", CommandType.EXIT, null, "EXIT()"));

            workerThread.join(3000);

            assertFalse(workerThread.isAlive());
            assertFalse(sessionState.isLoggedIn("user1"));

            Map<String, Integer> stats = repository.countModificationsPerUser();

            assertEquals(2, stats.get("user1"));
            assertFalse(stats.containsKey("user2"));

            assertTrue(output.messages.stream().anyMatch(m -> m.contains("Completed commandId=cmd-exit-007 EXIT")));
        }
    }

    private Command command(String commandId, CommandType type, String userId, String rawInput) {
        return new Command(commandId, type, userId, rawInput, Instant.now());
    }

    private static class TestOutputWriter implements OutputWriter {
        private final List<String> messages = new ArrayList<>();

        @Override
        public synchronized void write(String message) {
            messages.add(message);
        }
    }
}