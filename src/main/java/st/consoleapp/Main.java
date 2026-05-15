package st.consoleapp;

import st.consoleapp.command.CommandParser;
import st.consoleapp.config.AppConfig;
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

        OutputWriter output = new ConsoleOutputWriter();
        CommandQueue queue = new CommandQueue();

        UserSessionState sessionState = new UserSessionState();

        try (ModificationRepository repository = new JdbcModificationRepository(AppConfig.JDBC_URL)) {

            CommandProcessor processor = new CommandProcessor(sessionState, repository, output);

            CommandWorker worker = new CommandWorker(queue, processor);
            Thread workerThread = new Thread(worker, AppConfig.COMMAND_WORKER_THREAD_NAME);

            workerThread.start();

            CommandParser parser = new CommandParser();
            ConsoleCommandProducer producer = new ConsoleCommandProducer(parser, queue, output);

            producer.start();
            workerThread.join();
        }

        output.write("Application terminated.");
    }

}