package st.consoleapp.processing;

import st.consoleapp.command.Command;
import st.consoleapp.persistence.ModificationDao;
import st.consoleapp.persistence.UserDao;

import java.sql.SQLException;
import java.util.Map;

public class CommandProcessor {

    private final UserDao userDao;
    private final ModificationDao modificationDao;

    public CommandProcessor(UserDao userDao, ModificationDao modificationDao) {
        this.userDao = userDao;
        this.modificationDao = modificationDao;
    }

    public void process(Command command) {
        try {
            switch (command.type()) {
                case LOGIN:
                    userDao.login(command.userId());
                    break;
                case LOGOUT:
                    userDao.logout(command.userId());
                    break;
                case DATA_MODIFY:
                    if (userDao.isLoggedIn(command.userId())) {
                        modificationDao.addModification(command.userId());
                    }
                    break;
                case STATS:
                    int loggedInCount = userDao.getLoggedInCount();
                    System.out.println("Number of currently logged-in users: " + loggedInCount);
                    Map<String, Integer> modCounts = modificationDao.getModificationCounts();
                    System.out.println("Number of data modifications per user:");
                    for (Map.Entry<String, Integer> entry : modCounts.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    break;
                case EXIT:
                    // already handled
                    break;
                case INVALID:
                    // ignore
                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
