package st.consoleapp.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern USER_COMMAND =
            Pattern.compile("^(LOGIN|LOGOUT|DATA_MODIFY)\\(([^()]+)\\)$");

    private static final Pattern NO_ARG_COMMAND =
            Pattern.compile("^(STATS|EXIT)\\(\\)$");

    public ParsedCommand parse(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedCommand(CommandType.INVALID, null, String.valueOf(input));
        }

        String trimmed = input.trim();

        Matcher userMatcher = USER_COMMAND.matcher(trimmed);
        if (userMatcher.matches()) {
            CommandType type = CommandType.valueOf(userMatcher.group(1));
            String userId = userMatcher.group(2).trim();

            if (userId.isEmpty()) {
                return new ParsedCommand(CommandType.INVALID, null, trimmed);
            }

            return new ParsedCommand(type, userId, trimmed);
        }

        Matcher noArgMatcher = NO_ARG_COMMAND.matcher(trimmed);
        if (noArgMatcher.matches()) {
            CommandType type = CommandType.valueOf(noArgMatcher.group(1));
            return new ParsedCommand(type, null, trimmed);
        }

        return new ParsedCommand(CommandType.INVALID, null, trimmed);
    }
}