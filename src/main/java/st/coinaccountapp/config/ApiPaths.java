package st.coinaccountapp.config;

public class ApiPaths {
    public static final String API_VERSION = "v1";
    public static final String ACCOUNT_GET = "/" + API_VERSION + "/account/{guid}";
    public static final String TRANSACTION_POST = "/" + API_VERSION + "/account/tx";
}
