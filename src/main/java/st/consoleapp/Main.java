package st.consoleapp;

import st.consoleapp.command.Command;
import st.consoleapp.command.CommandParser;
import st.consoleapp.command.CommandType;
import st.consoleapp.persistence.DatabaseManager;
import st.consoleapp.persistence.ModificationDao;
import st.consoleapp.persistence.UserDao;
import st.consoleapp.processing.CommandProcessor;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            UserDao userDao = new UserDao(dbManager.getConnection());
            ModificationDao modificationDao = new ModificationDao(dbManager.getConnection());
            CommandProcessor processor = new CommandProcessor(userDao, modificationDao);
            ExecutorService executor = Executors.newFixedThreadPool(4);

            CommandParser parser = new CommandParser();
            System.out.println("Console App started. Type EXIT() to stop.");
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Command command = parser.parse(line);
                    System.out.println(command);
                    if (command.type() != CommandType.INVALID) {
                        executor.submit(() -> processor.process(command));
                    }
                    if (command.type() == CommandType.EXIT) {
                        break;
                    }
                }
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            dbManager.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}