package st.consoleapp.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for tracking logged-in users.
 * Represents application runtime session state.
 */
public class UserSessionState {

    private final Set<String> loggedUsers = ConcurrentHashMap.newKeySet();

    /**
     * Logs in a user if not already logged in.
     * @return true if login succeeded, false if user was already logged in
     */
    public boolean login(String userId) {
        return loggedUsers.add(userId);
    }

    /**
     * Logs out a user if currently logged in.
     * @return true if logout succeeded, false if user was not logged in
     */
    public boolean logout(String userId) {
        return loggedUsers.remove(userId);
    }

    /**
     * @return true if the user is logged in, false otherwise
     */
    public boolean isLoggedIn(String userId) {
        return loggedUsers.contains(userId);
    }

    /**
     * @return the count of logged-in users
     */
    public int getLoggedInUserCount() {
        return loggedUsers.size();
    }
}