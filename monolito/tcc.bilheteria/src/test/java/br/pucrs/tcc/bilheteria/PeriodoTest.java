package br.pucrs.tcc.bilheteria;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

class PeriodoTest {

    private final Instant inicio = Instant.parse("2026-11-01T10:00:00Z");
    private final Instant fim = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void inicioEInclusivoEFimEExclusivo() {
        Periodo periodo = Periodo.de(inicio, fim);

        assertThat(periodo.contem(inicio)).isTrue();
        assertThat(periodo.contem(inicio.plusSeconds(1))).isTrue();
        assertThat(periodo.contem(fim)).isFalse();
        assertThat(periodo.contem(inicio.minusSeconds(1))).isFalse();
    }

    @Test
    void informaSeIniciouOuEncerrou() {
        Periodo periodo = Periodo.de(inicio, fim);

        assertThat(periodo.iniciouEm(inicio)).isTrue();
        assertThat(periodo.iniciouEm(inicio.minusSeconds(1))).isFalse();
        assertThat(periodo.encerrouEm(fim)).isTrue();
        assertThat(periodo.encerrouEm(fim.minusSeconds(1))).isFalse();
    }

    @Test
    void calculaDuracao() {
        assertThat(Periodo.de(inicio, fim).duracao()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void rejeitaPeriodoInvertidoOuVazio() {
        assertThatExceptionOfType(RegraDeNegocioException.class)
                .isThrownBy(() -> Periodo.de(fim, inicio))
                .extracting(RegraDeNegocioException::codigo)
                .isEqualTo("periodo.invalido");
        assertThatExceptionOfType(RegraDeNegocioException.class)
                .isThrownBy(() -> Periodo.de(inicio, inicio))
                .extracting(RegraDeNegocioException::codigo)
                .isEqualTo("periodo.invalido");
    }

    @Test
    void rejeitaNulos() {
        assertThatThrownBy(() -> Periodo.de(null, fim)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Periodo.de(inicio, null)).isInstanceOf(NullPointerException.class);
    }
}