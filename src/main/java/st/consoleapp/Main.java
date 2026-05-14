package st.consoleapp;

import st.consoleapp.command.CommandParser;
import st.consoleapp.output.ConsoleOutputWriter;
import st.consoleapp.output.OutputWriter;
import st.consoleapp.persistence.InMemoryModificationRepository;
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
        ModificationRepository repository = new InMemoryModificationRepository();
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

        System.out.println("Application terminated.");
    }
}