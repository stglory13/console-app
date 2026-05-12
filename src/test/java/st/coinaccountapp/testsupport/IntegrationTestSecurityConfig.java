package st.coinaccountapp.testsupport;

import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Stub JwtDecoder pre integracne testy — akceptuje akykolvek string ako token
 * a vrati Jwt s rolami ADMIN + USER. Bez tohto by sa Spring snazil
 * fetchnut JWKS z reálneho Keycloaku.
 */
@TestConfiguration
public class IntegrationTestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("test-admin")
                .claim("preferred_username", "test-admin")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .build();
    }
}
