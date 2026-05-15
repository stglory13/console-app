package st.consoleapp.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void shouldParseLoginCommand() {
        ParsedCommand command = parser.parse("LOGIN(user1)");
        assertEquals(CommandType.LOGIN, command.type());
        assertEquals("user1", command.userId());
        assertEquals("LOGIN(user1)", command.rawInput());
    }

    @Test
    void shouldParseLogoutCommand() {
        ParsedCommand command = parser.parse("LOGOUT(user1)");
        assertEquals(CommandType.LOGOUT, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseDataModifyCommand() {
        ParsedCommand command = parser.parse("DATA_MODIFY(user1)");
        assertEquals(CommandType.DATA_MODIFY, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseStatsCommand() {
        ParsedCommand command = parser.parse("STATS()");
        assertEquals(CommandType.STATS, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldParseExitCommand() {
        ParsedCommand command = parser.parse("EXIT()");
        assertEquals(CommandType.EXIT, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldReturnInvalidForUnknownCommand() {
        ParsedCommand command = parser.parse("HELLO(user1)");
        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldReturnInvalidForBlankInput() {
        ParsedCommand command = parser.parse("   ");
        assertEquals(CommandType.INVALID, command.type());
    }
}