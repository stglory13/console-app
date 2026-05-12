package st.coinaccountapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurácia OpenAPI / Swagger UI.
 * Definuje meta-info API a Bearer JWT security schému, ktorá sa použije v Swagger UI cez tlačidlo „Authorize".
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coinAppOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Coinapp API").version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
