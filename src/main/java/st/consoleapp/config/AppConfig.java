package st.consoleapp.config;

public final class AppConfig {

    public static final String JDBC_URL = "jdbc:h2:mem:consoleapp;DB_CLOSE_DELAY=-1";

    public static final String COMMAND_WORKER_THREAD_NAME = "command-worker";

    public static final String USER_COMMAND_ID_PATTERN = "cmd-%s-%s-%03d";

    public static final String NO_USER_COMMAND_ID_PATTERN = "cmd-%s-%03d";

    private AppConfig() {
    }
}