package st.consoleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point Spring Boot aplikácie AlyApp.
 * Štartuje kontext, autokonfigurácie a embedded Tomcat.
 */
@SpringBootApplication
public class FlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlyApplication.class, args);
    }
}
