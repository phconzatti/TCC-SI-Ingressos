package br.pucrs.tcc.bilheteria.compartilhado.kernel;

import java.time.Instant;
import java.util.UUID;

// Contrato de todo evento trocado entre módulos. O id permite que consumidores sejam idempotentes na versão em microsserviços
public interface EventoDeIntegracao {
    UUID id();

    Instant ocorridoEm();
}
