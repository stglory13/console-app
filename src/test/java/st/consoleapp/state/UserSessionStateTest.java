package st.consoleapp.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSessionStateTest {

    @Test
    void shouldLoginUserOnlyOnce() {
        UserSessionState state = new UserSessionState();

        assertTrue(state.login("user1"));
        assertFalse(state.login("user1"));
        assertTrue(state.isLoggedIn("user1"));
        assertEquals(1, state.getLoggedInUserCount());
    }

    @Test
    void shouldLogoutOnlyLoggedInUser() {
        UserSessionState state = new UserSessionState();

        assertFalse(state.logout("user1"));

        state.login("user1");

        assertTrue(state.logout("user1"));
        assertFalse(state.isLoggedIn("user1"));
        assertEquals(0, state.getLoggedInUserCount());
    }
}