package st.consoleapp.output;

public class ConsoleOutputWriter implements OutputWriter {

    @Override
    public synchronized void write(String message) {
        System.out.println(message);
    }
}