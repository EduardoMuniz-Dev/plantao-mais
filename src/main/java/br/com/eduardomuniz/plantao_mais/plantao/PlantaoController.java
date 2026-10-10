package br.com.eduardomuniz.plantao_mais.plantao;

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
public class PlantaoController {

    private final PlantaoRepository repository;

    public PlantaoController(PlantaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PlantaoResponse> listar() {
        return repository.findAll().stream().map(PlantaoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantaoResponse> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(p -> ResponseEntity.ok(PlantaoResponse.de(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlantaoResponse> criar(@Valid @RequestBody PlantaoRequest dados) {
        Plantao salvo = repository.save(aplicar(new Plantao(), dados));
        URI local = URI.create("/plantoes/" + salvo.getId());
        return ResponseEntity.created(local).body(PlantaoResponse.de(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantaoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody PlantaoRequest dados) {
        return repository.findById(id)
                .map(existente -> ResponseEntity.ok(
                        PlantaoResponse.de(repository.save(aplicar(existente, dados)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
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
