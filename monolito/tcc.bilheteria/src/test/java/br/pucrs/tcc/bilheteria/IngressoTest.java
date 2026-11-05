package br.pucrs.tcc.bilheteria;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;
import br.pucrs.tcc.bilheteria.ingressos.domain.StatusIngresso;

class IngressoTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");

    private Ingresso novo() {
        return Ingresso.emitir(UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "COD-1", AGORA);
    }

    @Test
    void nasceEmitidoSemDataDeUso() {
        Ingresso i = novo();
        assertThat(i.status()).isEqualTo(StatusIngresso.EMITIDO);
        assertThat(i.utilizadoEm()).isNull();
    }

    @Test
    void rejeitaNumeroInvalidoECodigoEmBranco() {
        assertThatThrownBy(() -> Ingresso.emitir(UUID.randomUUID(), 0, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "x", AGORA))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.numero-invalido");
        assertThatThrownBy(() -> Ingresso.emitir(UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), " ", AGORA))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.codigo-obrigatorio");
    }

    @Test
    void checkInMarcaUtilizadoComADataEsoPodeAcontecerUmaVez() {
        Ingresso i = novo();
        Instant entrada = AGORA.plusSeconds(3600);

        i.realizarCheckIn(entrada);

        assertThat(i.status()).isEqualTo(StatusIngresso.UTILIZADO);
        assertThat(i.utilizadoEm()).isEqualTo(entrada);
        assertThatThrownBy(() -> i.realizarCheckIn(entrada.plusSeconds(1)))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.ja-utilizado");
        assertThat(i.utilizadoEm()).as("data original preservada").isEqualTo(entrada);
    }
}