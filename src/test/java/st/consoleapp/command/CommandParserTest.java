package st.consoleapp.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void shouldParseLoginCommand() {
        ParsedCommand command = parse("LOGIN(user1)");

        assertEquals(CommandType.LOGIN, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseLogoutCommand() {
        ParsedCommand command = parse("LOGOUT(user1)");

        assertEquals(CommandType.LOGOUT, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseDataModifyCommand() {
        ParsedCommand command = parse("DATA_MODIFY(user1)");

        assertEquals(CommandType.DATA_MODIFY, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseStatsCommand() {
        ParsedCommand command = parse("STATS()");

        assertEquals(CommandType.STATS, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldParseExitCommand() {
        ParsedCommand command = parse("EXIT()");

        assertEquals(CommandType.EXIT, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldReturnInvalidForUnknownCommand() {
        ParsedCommand command = parse("HELLO(user1)");

        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldReturnInvalidForBlankInput() {
        ParsedCommand command = parse("   ");

        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldReturnInvalidWhenUserMissing() {
        ParsedCommand command = parse("LOGIN()");

        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldReturnInvalidForMalformedInput() {
        ParsedCommand command = parse("LOGINuser1)");

        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldTrimInput() {
        ParsedCommand command = parse("   LOGIN(user1)   ");

        assertEquals(CommandType.LOGIN, command.type());
        assertEquals("user1", command.userId());
        assertEquals("LOGIN(user1)", command.rawInput());
    }

    // --- helper ---

    private ParsedCommand parse(String input) {
        return parser.parse(input);
    }
}