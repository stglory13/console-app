package st.consoleapp.producer;

import st.consoleapp.command.Command;
import st.consoleapp.command.CommandParser;
import st.consoleapp.command.ParsedCommand;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.queue.CommandQueue;

import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

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

    private String createCommandId(ParsedCommand command) {
        long number = sequence.getAndIncrement();
        String type = command.type().name().toLowerCase().replace("_", "-");

        if (command.userId() == null) {
            return String.format("cmd-%s-%03d", type, number);
        }

        return String.format("cmd-%s-%s-%03d", type, command.userId(), number);
    }
}