package st.consoleapp.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserSessionState {

    private final Set<String> loggedUsers = ConcurrentHashMap.newKeySet();

    public boolean login(String userId) {
        return loggedUsers.add(userId);
    }

    public boolean logout(String userId) {
        return loggedUsers.remove(userId);
    }

    public boolean isLoggedIn(String userId) {
        return loggedUsers.contains(userId);
    }

    public int getLoggedUserCount() {
        return loggedUsers.size();
    }
}