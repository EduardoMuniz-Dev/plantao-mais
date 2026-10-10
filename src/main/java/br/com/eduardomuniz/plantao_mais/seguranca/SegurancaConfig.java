package br.com.eduardomuniz.plantao_mais.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
                .requestMatchers(HttpMethod.GET, "/plantoes/**").hasAnyRole("COORDENADOR", "TECNICO")
                .requestMatchers("/plantoes/**").hasRole("COORDENADOR")
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(PasswordEncoder encoder,
            @Value("${plantao.seguranca.senha-coordenador:coord-dev}") String senhaCoordenador,
            @Value("${plantao.seguranca.senha-tecnico:tec-dev}") String senhaTecnico) {
        return new InMemoryUserDetailsManager(
            User.withUsername("coordenador")
                .password(encoder.encode(senhaCoordenador)).roles("COORDENADOR").build(),
            User.withUsername("tecnico")
                .password(encoder.encode(senhaTecnico)).roles("TECNICO").build());
    }
}
