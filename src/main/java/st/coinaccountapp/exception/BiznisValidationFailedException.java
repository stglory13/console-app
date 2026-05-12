package st.coinaccountapp.exception;

/**
 * Výnimka pre porušenie biznis pravidiel — napr. nedodržanie limitu prečerpania pri transakcii.
 * GlobalExceptionHandler ju mapuje na HTTP 400 Bad Request s message-om určeným pre frontend.
 */
public class BiznisValidationFailedException extends RuntimeException {

    public BiznisValidationFailedException(String message) {
        super(message);
    }
}
