package st.coinaccountapp.exception;

/**
 * Data not found exception
 */
public class NotFoundException extends RuntimeException {

    /**
     * @param identifier of required object.
     */
    public NotFoundException(Object identifier) {
        super(String.valueOf(identifier));
    }

    @Override
    public String getMessage() {
        return "Data not found: " + super.getMessage();
    }
}
