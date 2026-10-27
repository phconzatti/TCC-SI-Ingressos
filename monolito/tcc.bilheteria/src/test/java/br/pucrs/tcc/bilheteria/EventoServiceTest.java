package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucrs.tcc.bilheteria.eventos.application.AdicionarCategoria;
import br.pucrs.tcc.bilheteria.eventos.application.CriarEvento;
import br.pucrs.tcc.bilheteria.eventos.application.EventoService;
import br.pucrs.tcc.bilheteria.eventos.application.RepositorioDeEventos;

class EventoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DATA = AGORA.plus(Duration.ofDays(30));

    private final Map<UUID, Evento> banco = new HashMap<>();
    private final List<EventoDeIntegracao> publicados = new ArrayList<>();

    private final RepositorioDeEventos repositorio = new RepositorioDeEventos() {
        @Override
        public void salvar(Evento e) { banco.put(e.id(), e); }
        public Optional<Evento> buscar(UUID id) { return Optional.ofNullable(banco.get(id)); }
        public List<Evento> listar() { return List.copyOf(banco.values()); }
    };

    private final EventoService service = new EventoService(
            repositorio, publicados::add, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void publicarEmiteEventoComPrecoCapacidadeEJanelaDasCategorias() {
        Evento e = service.criar(new CriarEvento("Show", "Arena", DATA, 100));
        service.adicionarCategoria(e.id(),
                new AdicionarCategoria("Pista", 5000, 80, AGORA, DATA.minusSeconds(60)));

        service.publicar(e.id());

        assertThat(publicados).hasSize(1);
        EventoPublicado evento = (EventoPublicado) publicados.get(0);
        assertThat(evento.eventoId()).isEqualTo(e.id());
        assertThat(evento.categorias()).singleElement().satisfies(c -> {
            assertThat(c.precoEmCentavos()).isEqualTo(5000);
            assertThat(c.capacidade()).isEqualTo(80);
            assertThat(c.fimDasVendas()).isEqualTo(DATA.minusSeconds(60));
        });
    }

    @Test
    void naoEmiteEventoQuandoPublicacaoFalha() {
        Evento e = service.criar(new CriarEvento("Show", "Arena", DATA, 100));
        assertThatThrownBy(() -> service.publicar(e.id())).isInstanceOf(RuntimeException.class);
        assertThat(publicados).isEmpty();
    }

    @Test
    void eventoInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> service.buscar(UUID.randomUUID()))
                .isInstanceOf(NaoEncontradoException.class);
    }
}