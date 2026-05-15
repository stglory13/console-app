package st.consoleapp.producer;

import st.consoleapp.command.Command;
import st.consoleapp.command.CommandParser;
import st.consoleapp.command.CommandType;
import st.consoleapp.command.ParsedCommand;
import st.consoleapp.config.AppConfig;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.queue.CommandQueue;

import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads commands from console input and produces command events.
 * Acts as a producer in the event-driven processing pipeline.
 */
public class ConsoleCommandProducer {

    private final CommandParser parser;
    private final CommandQueue queue;
    private final OutputWriter output;
    private final AtomicLong sequence = new AtomicLong(1);

    public ConsoleCommandProducer(
            CommandParser parser,
            CommandQueue queue,
            OutputWriter output
    ) {
        this.parser = parser;
        this.queue = queue;
        this.output = output;
    }

    /**
     * Starts reading input from STDIN and submitting commands to the queue.
     */
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String line = scanner.nextLine();

                ParsedCommand parsed = parser.parse(line);
                String commandId = createCommandId(parsed);

                Command command = new Command(
                        commandId,
                        parsed.type(),
                        parsed.userId(),
                        parsed.rawInput(),
                        Instant.now()
                );

                output.write("Accepted commandId=" + command.commandId());

                queue.submit(command);

                if (command.type().name().equals("EXIT")) {
                    break;
                }
            }
        }
    }

    /**
     * Generates a unique command correlation ID.
     */
    private String createCommandId(ParsedCommand command) {
        long number = sequence.getAndIncrement();
        String normalizedTypeForCommandId = normalizeTypeForCommandId(command.type());

        if (command.userId() == null) {
            return String.format(
                    AppConfig.NO_USER_COMMAND_ID_PATTERN,
                    normalizedTypeForCommandId,
                    number
            );
        }
        return String.format(
                AppConfig.USER_COMMAND_ID_PATTERN,
                normalizedTypeForCommandId,
                command.userId(),
                number
        );
    }

    private String normalizeTypeForCommandId(CommandType type) {
        return type.name()
                .toLowerCase()
                .replace("_", "-");
    }
}