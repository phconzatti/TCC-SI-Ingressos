package br.pucrs.tcc.bilheteria.reservas.application;

import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

public record ReservarIngressos(UUID usuarioId, UUID categoriaId, int quantidade) {
    
    public ReservarIngressos {
        if (usuarioId == null || categoriaId == null) {
            throw new RegraDeNegocioException("reserva.dados-obrigatorios", "usuarioId e categoriaId são obrigatórios");
        }
    }
}
