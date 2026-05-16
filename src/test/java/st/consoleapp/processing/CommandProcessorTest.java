package st.consoleapp.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.consoleapp.command.Command;
import st.consoleapp.command.CommandType;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.JdbcModificationRepository;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.state.UserSessionState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandProcessorTest {

    private UserSessionState sessionState;
    private ModificationRepository repository;
    private TestOutputWriter output;
    private CommandProcessor processor;

    @BeforeEach
    void setUp() {
        sessionState = new UserSessionState();
        repository = new JdbcModificationRepository("jdbc:h2:mem:test-" + System.nanoTime());
        output = new TestOutputWriter();
        processor = new CommandProcessor(sessionState, repository, output);
    }

    @Test
    void shouldProcessLoginCommand() {
        Command command = command("cmd-login-user1-001", CommandType.LOGIN, "user1", "LOGIN(user1)");

        processor.process(command);

        assertTrue(sessionState.isLoggedIn("user1"));

        assertCompleted(command, "user logged in: user1");
    }

    @Test
    void shouldProcessLogoutCommand() {
        sessionState.login("user1");

        Command command = command("cmd-logout-user1-001", CommandType.LOGOUT, "user1", "LOGOUT(user1)");

        processor.process(command);

        assertFalse(sessionState.isLoggedIn("user1"));

        assertCompleted(command, "user logged out: user1");
    }

    @Test
    void shouldProcessDataModifyCommandForLoggedInUser() {
        sessionState.login("user1");

        Command command = command("cmd-data-modify-user1-001", CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)");

        processor.process(command);

        Map<String, Integer> stats = repository.countModificationsPerUser();

        assertEquals(1, stats.get("user1"));

        assertCompleted(command, "saved for user: user1");
    }

    @Test
    void shouldIgnoreDataModifyCommandForNotLoggedInUser() {
        Command command = command("cmd-data-modify-user1-001", CommandType.DATA_MODIFY, "user1", "DATA_MODIFY(user1)");

        processor.process(command);

        assertTrue(repository.countModificationsPerUser().isEmpty());

        assertCompleted(command, "ignored, user not logged in: user1");
    }

    @Test
    void shouldProcessStatsCommand() {
        sessionState.login("user1");
        sessionState.login("user2");

        repository.saveModification("cmd-data-modify-user1-001", "user1");
        repository.saveModification("cmd-data-modify-user1-002", "user1");

        Command command = command("cmd-stats-001", CommandType.STATS, null, "STATS()");

        processor.process(command);

        assertCompleted(command, "statistics printed");

        assertTrue(output.messages.stream()
                .anyMatch(m -> m.contains("Logged in users: 2")));

        assertTrue(output.messages.stream()
                .anyMatch(m -> m.contains("user1=2")));
    }

    @Test
    void shouldHandleInvalidCommand() {
        Command command = command("cmd-invalid-001", CommandType.INVALID, null, "HELLO()");

        processor.process(command);

        assertCompleted(command, "command ignored");
    }

    // --- Helpers ---

    private void assertCompleted(Command command, String text) {
        assertTrue(output.messages.stream().anyMatch(m ->
                m.contains("COMPLETED")
                        && m.contains("id=" + command.commandId())
                        && m.contains("cmd=" + command.type())
                        && m.contains(text)
        ), "Missing completed message for " + command.commandId());
    }

    private Command command(String commandId, CommandType type, String userId, String rawInput) {
        return new Command(commandId, type, userId, rawInput, Instant.now());
    }

    private static class TestOutputWriter implements OutputWriter {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void write(String message) {
            messages.add(message);
        }
    }
}