package st.coinaccountapp.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SLF4J markery pre kategorizáciu logov.
 * Rozlišujú biznis, payload a technické záznamy — možno podľa nich filtrovať a smerovať logy.
 */
public class LogsCategorization {

    public static final String BUSINESS_MARKER_NAME = "BIZ_MARK";
    public static final String PAYLOAD_MARKER_NAME = "PAYL_MARK";
    public static final String TECHNICAL_MARKER_NAME = "TECH_MARK";

    /**
     * Marker pre logovanie krokov biznis procesu, napr. „klient vytvorený“,
     * „dáta prijaté“, „dáta spracované“, „dáta uložené“ a podobne.
     */
    public static final Marker BUSINESS_MARKER = MarkerFactory.getMarker(BUSINESS_MARKER_NAME);

    /**
     * Marker pre logovanie requestov a response-ov (napr. SOAP payload)
     * pri komunikácii s inými modulmi alebo aplikáciami.
     */
    public static final Marker PAYLOAD_MARKER = MarkerFactory.getMarker(PAYLOAD_MARKER_NAME);

    /**
     * Marker pre technické logy, ktoré nespadajú pod biznis ani payload logy —
     * napríklad logovanie vstupov a výstupov dôležitých metód a podobne.
     */
    public static final Marker TECHNICAL_MARKER = MarkerFactory.getMarker(TECHNICAL_MARKER_NAME);
}
