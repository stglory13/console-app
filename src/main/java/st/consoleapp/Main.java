package st.consoleapp;

import st.consoleapp.command.CommandParser;
import st.consoleapp.output.ConsoleOutputWriter;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.JdbcModificationRepository;
import st.consoleapp.persistence.ModificationRepository;
import st.consoleapp.processing.CommandProcessor;
import st.consoleapp.producer.ConsoleCommandProducer;
import st.consoleapp.queue.CommandQueue;
import st.consoleapp.state.UserSessionState;
import st.consoleapp.worker.CommandWorker;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        CommandQueue queue = new CommandQueue();

        UserSessionState sessionState = new UserSessionState();

        try (ModificationRepository repository =
                     new JdbcModificationRepository("jdbc:h2:mem:consoleapp;DB_CLOSE_DELAY=-1")) {

            OutputWriter output = new ConsoleOutputWriter();

            CommandProcessor processor =
                    new CommandProcessor(sessionState, repository, output);

            CommandWorker worker = new CommandWorker(queue, processor);
            Thread workerThread = new Thread(worker, "command-worker");

            workerThread.start();

            CommandParser parser = new CommandParser();
            ConsoleCommandProducer producer =
                    new ConsoleCommandProducer(parser, queue);

            producer.start();

            workerThread.join();
        }

        System.out.println("Application terminated.");
    }

}