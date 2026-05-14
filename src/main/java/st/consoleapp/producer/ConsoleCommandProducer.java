package st.consoleapp.producer;

import st.consoleapp.command.Command;
import st.consoleapp.command.CommandParser;
import st.consoleapp.queue.CommandQueue;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class ConsoleCommandProducer {

    private final CommandParser parser;
    private final CommandQueue queue;
    private final AtomicLong sequence = new AtomicLong(1);

    public ConsoleCommandProducer(CommandParser parser, CommandQueue queue) {
        this.parser = parser;
        this.queue = queue;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String line = scanner.nextLine();

            Command parsed = parser.parse(line);

            Command command = new Command(
                    sequence.getAndIncrement(),
                    parsed.type(),
                    parsed.userId(),
                    parsed.rawInput()
            );

            queue.submit(command);

            if (command.type().name().equals("EXIT")) {
                break;
            }
        }
    }
}