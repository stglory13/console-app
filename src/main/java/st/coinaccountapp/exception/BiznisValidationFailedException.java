package st.coinaccountapp.exception;

/**
 * The business object is in a state where the requested operation cannot be performed.
 *
 * <p>This exception is specifically designed for interaction with the frontend to provide
 * a user-friendly presentation.</p>
 */
public class BiznisValidationFailedException extends RuntimeException {

    /**
     * @param message A message intended for the frontend, formatted as it should be displayed
     *                to the user (including diacritics, a user-friendly description, and
     *                properly structured sentences ending with a period, etc.).
     */
    public BiznisValidationFailedException(String message) {
        super(message);
    }

    /**
     * @param message A message intended for the frontend, formatted as it should be displayed
     *                to the user (including diacritics, a user-friendly description, and
     *                properly structured sentences ending with a period, etc.).
     * @param cause   The underlying cause of the exception.
     */
    public BiznisValidationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}