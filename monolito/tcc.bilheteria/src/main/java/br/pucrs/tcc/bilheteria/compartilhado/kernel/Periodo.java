package br.pucrs.tcc.bilheteria.compartilhado.kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;


// Intervalo de tempo com início inclusivo e fim exclusivo. O início é sempre anterior ao fim.
public record Periodo(Instant inicio, Instant fim) {
    
    public Periodo {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!inicio.isBefore(fim)) {
            throw new RegraDeNegocioException("periodo.invalido", "O início do período deve ser anterior ao fim");
        }
    }

    public static Periodo de(Instant inicio, Instant fim) {
        return new Periodo(inicio, fim);
    }

    // Verdadeiro se o instante está em [inicio, fim)
    public boolean contem(Instant instante) {
        return !instante.isBefore(inicio) && instante.isBefore(fim);
    }

    public boolean iniciouEm(Instant agora) {
        return !agora.isBefore(inicio);
    }

    public boolean encerrouEm(Instant agora) {
        return !agora.isBefore(fim);
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }
}
