package br.com.eduardomuniz.plantao_mais.seguranca;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/plantoes/**").hasAnyRole("COORDENADOR", "TECNICO")
                .requestMatchers("/plantoes/**").hasRole("COORDENADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePapeis())));
        return http.build();
    }

    /** Converte realm_access.roles do token do Keycloak em ROLE_COORDENADOR, ROLE_TECNICO etc. */
    private JwtAuthenticationConverter conversorDePapeis() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> acesso = jwt.getClaimAsMap("realm_access");
            if (acesso == null || !(acesso.get("roles") instanceof Collection<?> papeis)) {
                return List.<GrantedAuthority>of();
            }
            return papeis.stream()
                    .map(papel -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + papel))
                    .toList();
        });
        return conversor;
    }
}
