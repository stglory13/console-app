package st.consoleapp.command;

import java.util.Objects;

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