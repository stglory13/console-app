package st.consoleapp.worker;

import st.consoleapp.command.Command;
import st.consoleapp.processing.CommandProcessor;
import st.consoleapp.queue.CommandQueue;

public class CommandWorker implements Runnable {

    private final CommandQueue queue;
    private final CommandProcessor processor;
    private volatile boolean running = true;

    public CommandWorker(CommandQueue queue, CommandProcessor processor) {
        this.queue = queue;
        this.processor = processor;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Command command = queue.take();
                processor.process(command);

                if (command.type().name().equals("EXIT")) {
                    shutdown();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shutdown();
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}