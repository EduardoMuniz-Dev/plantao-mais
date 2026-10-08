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
    public List<Plantao> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plantao> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Plantao> criar(@Valid @RequestBody Plantao plantao) {
        Plantao salvo = repository.save(plantao);
        URI local = URI.create("/plantoes/" + salvo.getId());
        return ResponseEntity.created(local).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plantao> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Plantao dados) {
        return repository.findById(id)
                .map(existente -> {
                    existente.setSetor(dados.getSetor());
                    existente.setProfissional(dados.getProfissional());
                    existente.setInicio(dados.getInicio());
                    existente.setFim(dados.getFim());
                    return ResponseEntity.ok(repository.save(existente));
                })
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
}
