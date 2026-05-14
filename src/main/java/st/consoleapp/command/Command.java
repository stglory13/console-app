package st.consoleapp.command;

public record Command(CommandType type, String userId, String rawInput) {

}
