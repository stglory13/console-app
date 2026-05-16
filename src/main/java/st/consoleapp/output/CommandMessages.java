package st.consoleapp.output;

import st.consoleapp.command.Command;
import st.consoleapp.config.AppConfig;

public final class CommandMessages {

    private CommandMessages() {
    }

    public static final String USER_LOGGED_IN = "user logged in";
    public static final String USER_ALREADY_LOGGED_IN = "user already logged in";
    public static final String USER_LOGGED_OUT = "user logged out";
    public static final String USER_NOT_LOGGED_IN = "user not logged in";

    public static final String MODIFICATION_SAVED = "saved for user";
    public static final String MODIFICATION_IGNORED = "ignored, user not logged in";

    public static final String STATS_PRINTED = "statistics printed";
    public static final String STATS_LOGGED_USERS = "Logged in users: ";
    public static final String STATS_MODIFICATIONS = "Data modifications per user: ";

    public static final String SHUTDOWN_REQUESTED = "shutdown requested";
    public static final String COMMAND_IGNORED = "command ignored";
    public static final String ERROR_PREFIX = "error";

    public static String accepted(Command command) {
        return String.format(
                AppConfig.ACCEPTED_OUTPUT_PATTERN,
                command.commandId(),
                command.type()
        );
    }

    public static String completed(Command command, String message) {
        return String.format(
                AppConfig.COMPLETED_OUTPUT_PATTERN,
                command.commandId(),
                command.type(),
                message
        );
    }

    public static String forUser(String message, String userId) {
        return message + ": " + userId;
    }
}