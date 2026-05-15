package st.consoleapp.worker;

import st.consoleapp.command.Command;
import st.consoleapp.command.CommandType;
import st.consoleapp.processing.CommandProcessor;
import st.consoleapp.queue.CommandQueue;

/**
 * Consumes command events from the queue and executes them using the processor.
 * Responsible for asynchronous command handling.
 */
public class CommandWorker implements Runnable {

    private final CommandQueue queue;
    private final CommandProcessor processor;
    private volatile boolean running = true;

    public CommandWorker(CommandQueue queue, CommandProcessor processor) {
        this.queue = queue;
        this.processor = processor;
    }

    /**
     * Starts consuming commands from the queue until shutdown is triggered.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Command command = queue.take();
                processor.process(command);
                if (command.type() == CommandType.EXIT) {
                    shutdown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shutdown();
            }
        }
    }

    /**
     * Stops the worker and terminates processing loop
     */
    public void shutdown() {
        running = false;
    }
}