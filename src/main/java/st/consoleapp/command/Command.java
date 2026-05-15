package st.consoleapp.command;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable command representing a user action.
 * Acts as an event in the application's event-driven processing pipeline.
 *
 * @param commandId correlation ID used to track the command across async processing (not a DB primary key)
 * @param type parsed command type
 * @param userId user identifier, null for commands without user context
 * @param rawInput original console input
 * @param createdAt timestamp when the command was created
 */
public record Command(
        String commandId,
        CommandType type,
        String userId,
        String rawInput,
        Instant createdAt
) {
    public Command {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(rawInput, "rawInput must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}