package st.flyapp.config;

/**
 * Konštanty s URL cestami REST API.
 * Centralizuje verziu API a tvary endpointov, aby boli použité konzistentne v kontroléri aj testoch.
 */
public class ApiPaths {
    public static final String API = "v1";
    public static final String API_V1 = API + "/v1";
    public static final String ACCOUNT_GET = "/" + API_V1 + "/accounts/{guid}";
    public static final String TRANSACTION_POST = "/" + API_V1 + "/account/tx";
}
