package st.consoleapp.command;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable command representing user input.
 */
public record Command(
        String commandId,   // business ID (correlation ID for async processing)
        CommandType type,   // parsed command type
        String userId,      // user id, null only for STATS/EXIT
        String rawInput,    // original console input, examples: "STATS()", "LOGIN(user1)", "LOGOUT(user1)", "DATA_MODIFY(user1)", "EXIT()"
        Instant createdAt   // creation timestamp
) {
    public Command {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(rawInput, "rawInput must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}