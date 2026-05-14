package st.flyapp.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security konfigurácia aplikácie — Spring Security 6 + OAuth2 Resource Server.
 * Stateless autentifikácia cez JWT (Keycloak), CSRF vypnuté, verejné endpointy pre health a OpenAPI,
 * všetko ostatné vyžaduje validný token. Role z claim-u realm_access.roles sa mapujú na ROLE_X authorities.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Hlavný Spring Security filter chain — definuje verejné endpointy, vyžaduje JWT pre ostatné
     * a registruje konverter rolí.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // verejné — health pre Docker / k8s probes, OpenAPI dokumentácia
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        // ostatné actuator endpointy (env, configprops, beans, mappings, ...)
                        // expozované iba v profile localdev — vyžadujú rolu ADMIN
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Konverter JWT na Authentication — extrahuje role cez KeycloakRealmRoleConverter
     * a ako principal name použije claim preferred_username.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    /**
     * Konverter rolí z Keycloak JWT na Spring Security authorities.
     * Každú rolu z claim-u realm_access.roles prefixuje „ROLE_", aby fungovalo hasRole('X').
     * Doplňuje aj štandardné scope authorities z OAuth2.
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>(scopeConverter.convert(jwt));

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String name) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }
            return authorities;
        }
    }
}
