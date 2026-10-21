package br.com.eduardomuniz.plantao_mais.plantao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plantoes")
@Tag(name = "Plantões", description = "Gestão das escalas de plantão")
public class PlantaoController {

    private final PlantaoRepository repository;

    public PlantaoController(PlantaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Lista todos os plantões",
            description = "Disponível para os papéis COORDENADOR e TECNICO.")
    @ApiResponse(responseCode = "200", description = "Lista de plantões (pode ser vazia)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlantaoResponse.class))))
    public List<PlantaoResponse> listar() {
        return repository.findAll().stream().map(PlantaoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um plantão pelo id",
            description = "Disponível para os papéis COORDENADOR e TECNICO.")
    @ApiResponse(responseCode = "200", description = "Plantão encontrado",
            content = @Content(schema = @Schema(implementation = PlantaoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Plantão não encontrado", content = @Content)
    public ResponseEntity<PlantaoResponse> buscar(
            @Parameter(description = "Identificador do plantão", example = "1") @PathVariable Long id) {
        return repository.findById(id)
                .map(p -> ResponseEntity.ok(PlantaoResponse.de(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Cadastra um plantão",
            description = "Somente COORDENADOR. O cabeçalho Location aponta para o novo plantão.")
    @ApiResponse(responseCode = "201", description = "Plantão criado",
            content = @Content(schema = @Schema(implementation = PlantaoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (indica cada campo com problema)",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige o papel COORDENADOR)",
            content = @Content)
    public ResponseEntity<PlantaoResponse> criar(@Valid @RequestBody PlantaoRequest dados) {
        Plantao salvo = repository.save(aplicar(new Plantao(), dados));
        URI local = URI.create("/plantoes/" + salvo.getId());
        return ResponseEntity.created(local).body(PlantaoResponse.de(salvo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um plantão",
            description = "Somente COORDENADOR. Substitui o plantão inteiro.")
    @ApiResponse(responseCode = "200", description = "Plantão atualizado",
            content = @Content(schema = @Schema(implementation = PlantaoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige o papel COORDENADOR)",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Plantão não encontrado", content = @Content)
    public ResponseEntity<PlantaoResponse> atualizar(
            @Parameter(description = "Identificador do plantão", example = "1") @PathVariable Long id,
            @Valid @RequestBody PlantaoRequest dados) {
        return repository.findById(id)
                .map(existente -> ResponseEntity.ok(
                        PlantaoResponse.de(repository.save(aplicar(existente, dados)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um plantão", description = "Somente COORDENADOR.")
    @ApiResponse(responseCode = "204", description = "Plantão removido", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sem permissão (exige o papel COORDENADOR)",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Plantão não encontrado", content = @Content)
    public ResponseEntity<Void> remover(
            @Parameter(description = "Identificador do plantão", example = "1") @PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Plantao aplicar(Plantao plantao, PlantaoRequest dados) {
        plantao.setSetor(dados.setor());
        plantao.setProfissional(dados.profissional());
        plantao.setInicio(dados.inicio());
        plantao.setFim(dados.fim());
        return plantao;
    }
}
