package br.com.eduardomuniz.plantao_mais.plantao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlantaoControllerTest {

    private static final String PLANTAO_VALIDO = """
            {
              "setor": "UTI",
              "profissional": "Maria Silva",
              "inicio": "2026-10-20T07:00:00",
              "fim": "2026-10-20T19:00:00"
            }
            """;

    private static final String PLANTAO_INVALIDO = """
            {
              "setor": "",
              "profissional": "Maria Silva",
              "inicio": "2026-10-20T07:00:00",
              "fim": "2026-10-20T19:00:00"
            }
            """;

    /**
     * Decodificador de teste: "coordenador" e "tecnico" viram tokens com o papel
     * correspondente em realm_access.roles, como o Keycloak emite. Qualquer outro
     * valor é rejeitado, como seria um token inválido. Assim os testes não
     * dependem de um Keycloak rodando.
     */
    @TestConfiguration
    static class JwtDeTeste {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                List<String> papeis = switch (token) {
                    case "coordenador" -> List.of("COORDENADOR");
                    case "tecnico" -> List.of("TECNICO");
                    default -> throw new BadJwtException("Token inválido");
                };
                Instant agora = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject(token)
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(300))
                        .claim("realm_access", Map.of("roles", papeis))
                        .build();
            };
        }
    }

    @Autowired
    private MockMvc mvc;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/plantoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/plantoes").header("Authorization", bearer("invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tecnicoPodeListar() throws Exception {
        mvc.perform(get("/plantoes").header("Authorization", bearer("tecnico")))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoNaoPodeCriar() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", bearer("tecnico"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordenadorPodeCriar() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", bearer("coordenador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.setor").value("UTI"));
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", bearer("coordenador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_INVALIDO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        mvc.perform(get("/plantoes/999999").header("Authorization", bearer("coordenador")))
                .andExpect(status().isNotFound());
    }

    @Test
    void coordenadorPodeRemoverPlantao() throws Exception {
        String local = mvc.perform(post("/plantoes")
                        .header("Authorization", bearer("coordenador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertNotNull(local);

        mvc.perform(delete(local).header("Authorization", bearer("coordenador")))
                .andExpect(status().isNoContent());
        mvc.perform(get(local).header("Authorization", bearer("coordenador")))
                .andExpect(status().isNotFound());
    }
}
