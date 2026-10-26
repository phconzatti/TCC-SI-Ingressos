package br.pucrs.tcc.bilheteria.compartilhado.contratos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;

// Emitido por Eventos ao publicar um evento. Reservas usa para criar Disponibilidade

public record EventoPublicado (

    UUID id,
    Instant ocorridoEm,
    UUID eventoId,
    String nome,
    Instant dataDoEvento,
    List<CategoriaPublicada> categorias ) implements EventoDeIntegracao {

        public record CategoriaPublicada(

            UUID categoriaId,
            String nome,
            long precoEmCentavos,
            int capacidade,
            Instant inicioDasVendas,
            Instant fimDasVendas) {
                
            }
        
    }
