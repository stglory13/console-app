package st.coinaccountapp.exception;

/**
 * Výnimka pre prípad, že požadovaná entita sa nenašla v DB.
 * GlobalExceptionHandler ju mapuje na HTTP 404 Not Found.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(Object identifier) {
        super(String.valueOf(identifier));
    }

    @Override
    public String getMessage() {
        return "Data not found: " + super.getMessage();
    }
}
