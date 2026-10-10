package br.com.eduardomuniz.plantao_mais.plantao;

import java.time.LocalDateTime;

public record PlantaoResponse(
        Long id,
        String setor,
        String profissional,
        LocalDateTime inicio,
        LocalDateTime fim) {

    public static PlantaoResponse de(Plantao p) {
        return new PlantaoResponse(p.getId(), p.getSetor(), p.getProfissional(),
                p.getInicio(), p.getFim());
    }
}
