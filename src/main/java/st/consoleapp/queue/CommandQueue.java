package st.consoleapp.queue;

import st.consoleapp.command.Command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CommandQueue {

    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();

    public void submit(Command command) {
        queue.offer(command);
    }

    public Command take() throws InterruptedException {
        return queue.take();
    }
}