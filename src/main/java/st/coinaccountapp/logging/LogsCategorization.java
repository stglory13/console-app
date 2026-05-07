package st.coinaccountapp.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * This class defines markers for separating different types of logs.
 * Logs are categorized into different categories – a more detailed description
 * can be found below for each specific marker.
 *
 */
public class LogsCategorization {

    // String names are adjusted to the same length for better readability in logs.
    // We are aware of the grammatical incorrectness of the term "BUSINES" – this is intentional,
    // see the rule mentioned above.
    public static final String BUSINESS_MARKER_NAME     = "BIZ_MARK";
    public static final String PAYLOAD_MARKER_NAME      = "PAYL_MARK";
    public static final String TECHNICAL_MARKER_NAME    = "TECH_MARK";

    /**
     * Marker for logging process steps, e.g., "client created", "data received",
     * "data processed", "data stored", etc.
     */
    public static final Marker BUSINESS_MARKER = MarkerFactory.getMarker(BUSINESS_MARKER_NAME);

    /**
     * Marker for logging requests and responses (e.g., SOAP payload) during
     * communication with other modules or applications.
     */
    public static final Marker PAYLOAD_MARKER = MarkerFactory.getMarker(PAYLOAD_MARKER_NAME);

    /**
     * Marker for logs that do not fall under business or payload logs.
     * For example, logging inputs and outputs of important methods, etc.
     */
    public static final Marker TECHNICAL_MARKER = MarkerFactory.getMarker(TECHNICAL_MARKER_NAME);
}
