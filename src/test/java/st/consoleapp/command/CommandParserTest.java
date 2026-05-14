package st.consoleapp.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void shouldParseLoginCommand() {
        Command command = parser.parse("LOGIN(user1)");

        assertEquals(CommandType.LOGIN, command.type());
        assertEquals("user1", command.userId());
        assertEquals("LOGIN(user1)", command.rawInput());
    }

    @Test
    void shouldParseLogoutCommand() {
        Command command = parser.parse("LOGOUT(user1)");

        assertEquals(CommandType.LOGOUT, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseDataModifyCommand() {
        Command command = parser.parse("DATA_MODIFY(user1)");

        assertEquals(CommandType.DATA_MODIFY, command.type());
        assertEquals("user1", command.userId());
    }

    @Test
    void shouldParseStatsCommand() {
        Command command = parser.parse("STATS()");

        assertEquals(CommandType.STATS, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldParseExitCommand() {
        Command command = parser.parse("EXIT()");

        assertEquals(CommandType.EXIT, command.type());
        assertNull(command.userId());
    }

    @Test
    void shouldReturnInvalidForUnknownCommand() {
        Command command = parser.parse("HELLO(user1)");

        assertEquals(CommandType.INVALID, command.type());
    }

    @Test
    void shouldReturnInvalidForBlankInput() {
        Command command = parser.parse("   ");

        assertEquals(CommandType.INVALID, command.type());
    }
}