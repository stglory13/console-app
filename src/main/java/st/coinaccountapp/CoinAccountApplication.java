package st.coinaccountapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point Spring Boot aplikácie CoinApp.
 * Štartuje kontext, autokonfigurácie a embedded Tomcat.
 */
@SpringBootApplication
public class CoinAccountApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoinAccountApplication.class, args);
    }
}
