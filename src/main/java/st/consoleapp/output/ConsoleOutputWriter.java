package st.consoleapp.output;

import st.consoleapp.config.AppConfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ConsoleOutputWriter implements OutputWriter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public synchronized void write(String message) {
        System.out.println(String.format(
                AppConfig.CONSOLE_OUTPUT_PREFIX_PATTERN,
                AppConfig.APP_TAG,
                LocalTime.now().format(TIME_FORMAT),
                message
        ));
    }
}