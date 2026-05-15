package st.consoleapp.queue;

import st.consoleapp.command.Command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe command queue used to decouple input reading from processing.
 * Commands are produced by the console layer and consumed by worker threads.
 */
public class CommandQueue {

    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();

    /**
     * Submits a command to the queue for asynchronous processing.
     */
    public void submit(Command command) {
        queue.offer(command);
    }

    /**
     * Retrieves and removes the next command from the queue,
     * blocking if necessary until one is available.
     */
    public Command take() throws InterruptedException {
        return queue.take();
    }
}