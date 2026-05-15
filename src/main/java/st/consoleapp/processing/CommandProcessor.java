package st.consoleapp.processing;

import st.consoleapp.command.Command;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.state.UserSessionState;

/**
 * Processes command events and executes corresponding business logic.
 * Acts as a consumer in the producer-consumer event-driven architecture.
 */
public class CommandProcessor {

    private final UserSessionState sessionState;
    private final ModificationRepository repository;
    private final OutputWriter output;

    /**
     * Initializes processor with session state, repository and output.
     */
    public CommandProcessor(
            UserSessionState sessionState,
            ModificationRepository repository,
            OutputWriter output
    ) {
        this.sessionState = sessionState;
        this.repository = repository;
        this.output = output;
    }

    /**
     * Processes a single command event.
     */
    public void process(Command command) {
        try {
            switch (command.type()) {
                case LOGIN -> processLogin(command);
                case LOGOUT -> processLogout(command);
                case DATA_MODIFY -> processDataModify(command);
                case STATS -> processStats(command);
                case EXIT -> output.write("Completed commandId=" + command.commandId() + " EXIT");
                case INVALID -> output.write("Invalid command ignored: " + command.rawInput());
            }
        } catch (Exception e) {
            output.write("Failed commandId=" + command.commandId() + " " + command.rawInput());
        }
    }

    /**
     * Handles LOGIN command.
     */
    private void processLogin(Command command) {
        boolean success = sessionState.login(command.userId());

        if (success) {
            output.write("Completed commandId=" + command.commandId() + " LOGIN: user logged in: " + command.userId());
        } else {
            output.write("Completed commandId=" + command.commandId() + " LOGIN: user already logged in: " + command.userId());
        }
    }

    /**
     * Handles LOGOUT command.
     */
    private void processLogout(Command command) {
        boolean success = sessionState.logout(command.userId());

        if (success) {
            output.write("Completed commandId=" + command.commandId() + " LOGOUT: user logged out: " + command.userId());
        } else {
            output.write("Completed commandId=" + command.commandId() + " LOGOUT: user is not logged in: " + command.userId());
        }
    }

    /**
     * Handles DATA_MODIFY command.
     */
    private void processDataModify(Command command) {
        if (!sessionState.isLoggedIn(command.userId())) {
            output.write("Completed commandId=" + command.commandId() + " DATA_MODIFY: ignored, user not logged in: " + command.userId());
            return;
        }

        repository.saveModification(command.commandId(), command.userId());
        output.write("Completed commandId=" + command.commandId() + " DATA_MODIFY: saved for user: " + command.userId());
    }

    /**
     * Handles STATS command.
     */
    private void processStats(Command command) {
        output.write("Completed commandId=" + command.commandId() + " STATS");
        output.write("Logged in users: " + sessionState.getLoggedInUserCount());
        output.write("Data modifications per user: " + repository.countModificationsPerUser());
    }
}