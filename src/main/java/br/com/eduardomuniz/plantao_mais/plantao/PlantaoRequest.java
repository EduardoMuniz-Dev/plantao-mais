package br.com.eduardomuniz.plantao_mais.plantao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PlantaoRequest(
        @NotBlank String setor,
        @NotBlank String profissional,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim) {
}
