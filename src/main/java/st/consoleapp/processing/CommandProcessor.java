package st.consoleapp.processing;

import st.consoleapp.command.Command;
import st.consoleapp.output.CommandMessages;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.state.UserSessionState;

import static st.consoleapp.output.CommandMessages.COMMAND_IGNORED;
import static st.consoleapp.output.CommandMessages.ERROR_PREFIX;
import static st.consoleapp.output.CommandMessages.MODIFICATION_IGNORED;
import static st.consoleapp.output.CommandMessages.MODIFICATION_SAVED;
import static st.consoleapp.output.CommandMessages.SHUTDOWN_REQUESTED;
import static st.consoleapp.output.CommandMessages.STATS_LOGGED_USERS;
import static st.consoleapp.output.CommandMessages.STATS_MODIFICATIONS;
import static st.consoleapp.output.CommandMessages.STATS_PRINTED;
import static st.consoleapp.output.CommandMessages.USER_ALREADY_LOGGED_IN;
import static st.consoleapp.output.CommandMessages.USER_LOGGED_IN;
import static st.consoleapp.output.CommandMessages.USER_LOGGED_OUT;
import static st.consoleapp.output.CommandMessages.USER_NOT_LOGGED_IN;

/**
 * Processes command events and applies business logic.
 */
public class CommandProcessor {

    private final UserSessionState sessionState;
    private final ModificationRepository repository;
    private final OutputWriter output;

    public CommandProcessor(UserSessionState sessionState,
                            ModificationRepository repository,
                            OutputWriter output) {
        this.sessionState = sessionState;
        this.repository = repository;
        this.output = output;
    }

    public void process(Command command) {
        try {
            switch (command.type()) {
                case LOGIN -> processLogin(command);
                case LOGOUT -> processLogout(command);
                case DATA_MODIFY -> processDataModify(command);
                case STATS -> processStats(command);
                case EXIT -> completed(command, SHUTDOWN_REQUESTED);
                case INVALID -> completed(command, COMMAND_IGNORED + ": " + command.rawInput());
                default -> throw new IllegalStateException("Unsupported command type: " + command.type());
            }
        } catch (Exception e) {
            completed(command, ERROR_PREFIX + ": " + e.getMessage());
        }
    }

    private void processLogin(Command command) {
        boolean success = sessionState.login(command.userId());
        completedForUser(command, success ? USER_LOGGED_IN : USER_ALREADY_LOGGED_IN);
    }

    private void processLogout(Command command) {
        boolean success = sessionState.logout(command.userId());
        completedForUser(command, success ? USER_LOGGED_OUT : USER_NOT_LOGGED_IN);
    }

    private void processDataModify(Command command) {
        if (!sessionState.isLoggedIn(command.userId())) {
            completedForUser(command, MODIFICATION_IGNORED);
            return;
        }

        repository.saveModification(command.commandId(), command.userId());
        completedForUser(command, MODIFICATION_SAVED);
    }

    private void processStats(Command command) {
        completed(command, STATS_PRINTED);

        output.write(STATS_LOGGED_USERS + sessionState.getLoggedInUserCount());
        output.write(STATS_MODIFICATIONS + repository.countModificationsPerUser());
    }

    private void completedForUser(Command command, String message) {
        completed(command, CommandMessages.forUser(message, command.userId()));
    }

    private void completed(Command command, String message) {
        output.write(CommandMessages.completed(command, message));
    }
}