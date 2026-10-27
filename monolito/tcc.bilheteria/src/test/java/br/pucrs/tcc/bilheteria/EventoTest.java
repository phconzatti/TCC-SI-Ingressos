package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import br.pucrs.tcc.bilheteria.eventos.domain.StatusEvento;

class EventoTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DATA = AGORA.plus(Duration.ofDays(30));
    private static final Periodo JANELA = Periodo.de(AGORA, DATA.minusSeconds(3600));

    private Evento novoEvento(int capacidade) {
        return Evento.criar("Show", "Arena", DATA, capacidade, AGORA);
    }

    @Test
    void criaComoRascunho() {
        assertThat(novoEvento(100).status()).isEqualTo(StatusEvento.RASCUNHO);
    }

    @Test
    void rejeitaDataNoPassado() {
        assertThatThrownBy(() -> Evento.criar("Show", "Arena", AGORA.minusSeconds(1), 100, AGORA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "evento.data-invalida");
    }

    @Test
    void rejeitaCategoriasQueExcedemACapacidadeDoEvento() {
        Evento e = novoEvento(100);
        e.adicionarCategoria("Pista", Dinheiro.deCentavos(5000), 60, JANELA);
        assertThatThrownBy(() -> e.adicionarCategoria("VIP", Dinheiro.deCentavos(9000), 50, JANELA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "evento.capacidade-excedida");
    }

    @Test
    void aceitaCategoriasAteOLimiteExato() {
        Evento e = novoEvento(100);
        e.adicionarCategoria("Pista", Dinheiro.deCentavos(5000), 60, JANELA);
        e.adicionarCategoria("VIP", Dinheiro.deCentavos(9000), 40, JANELA);
        assertThat(e.capacidadeAlocada()).isEqualTo(100);
    }

    @Test
    void rejeitaVendaEncerrandoDepoisDoEvento() {
        Evento e = novoEvento(100);
        Periodo tarde = Periodo.de(AGORA, DATA.plusSeconds(1));
        assertThatThrownBy(() -> e.adicionarCategoria("Pista", Dinheiro.deCentavos(5000), 10, tarde))
                .hasFieldOrPropertyWithValue("codigo", "categoria.janela-apos-evento");
    }

    @Test
    void rejeitaNomeDeCategoriaDuplicado() {
        Evento e = novoEvento(100);
        e.adicionarCategoria("Pista", Dinheiro.deCentavos(5000), 10, JANELA);
        assertThatThrownBy(() -> e.adicionarCategoria("pista", Dinheiro.deCentavos(5000), 10, JANELA))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("codigo", "categoria.nome-duplicado");
    }

    @Test
    void naoPublicaSemCategorias() {
        assertThatThrownBy(() -> novoEvento(100).publicar())
                .hasFieldOrPropertyWithValue("codigo", "evento.sem-categorias");
    }

    @Test
    void publicaUmaVezEBloqueiaEdicao() {
        Evento e = novoEvento(100);
        e.adicionarCategoria("Pista", Dinheiro.deCentavos(5000), 10, JANELA);
        e.publicar();
        assertThat(e.status()).isEqualTo(StatusEvento.PUBLICADO);
        assertThatThrownBy(e::publicar).hasFieldOrPropertyWithValue("codigo", "evento.ja-publicado");
        assertThatThrownBy(() -> e.adicionarCategoria("VIP", Dinheiro.deCentavos(9000), 10, JANELA))
                .hasFieldOrPropertyWithValue("codigo", "evento.nao-editavel");
    }
}