package st.consoleapp.command;

import java.util.Objects;

/**
 * Represents the result of parsing raw console input.
 * Used as an intermediate object before creating a Command event.
 *
 * @param type parsed command type
 * @param userId extracted user identifier, null for commands without user context
 * @param rawInput original console input
 */
public record ParsedCommand(
        CommandType type,
        String userId,
        String rawInput
) {
    public ParsedCommand {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(rawInput, "rawInput must not be null");
    }
}