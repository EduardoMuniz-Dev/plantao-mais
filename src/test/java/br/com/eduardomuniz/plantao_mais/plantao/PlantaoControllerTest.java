package br.com.eduardomuniz.plantao_mais.plantao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    @Autowired
    private MockMvc mvc;

    private static String basic(String usuario, String senha) {
        String credenciais = usuario + ":" + senha;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }

    private static String coordenador() {
        return basic("coordenador", "coord-dev");
    }

    private static String tecnico() {
        return basic("tecnico", "tec-dev");
    }

    @Test
    void semLoginRetorna401() throws Exception {
        mvc.perform(get("/plantoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(get("/plantoes").header("Authorization", basic("coordenador", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tecnicoPodeListar() throws Exception {
        mvc.perform(get("/plantoes").header("Authorization", tecnico()))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoNaoPodeCriar() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", tecnico())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordenadorPodeCriar() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", coordenador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.setor").value("UTI"));
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        mvc.perform(post("/plantoes")
                        .header("Authorization", coordenador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_INVALIDO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        mvc.perform(get("/plantoes/999999").header("Authorization", coordenador()))
                .andExpect(status().isNotFound());
    }

    @Test
    void coordenadorPodeRemoverPlantao() throws Exception {
        String local = mvc.perform(post("/plantoes")
                        .header("Authorization", coordenador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLANTAO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertNotNull(local);

        mvc.perform(delete(local).header("Authorization", coordenador()))
                .andExpect(status().isNoContent());
        mvc.perform(get(local).header("Authorization", coordenador()))
                .andExpect(status().isNotFound());
    }
}
