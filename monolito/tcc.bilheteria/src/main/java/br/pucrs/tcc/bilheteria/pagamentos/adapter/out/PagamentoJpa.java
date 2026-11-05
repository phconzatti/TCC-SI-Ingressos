package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagamento", schema = "pagamentos")
class PagamentoJpa {

    @Id
    UUID id;

    @Column(name = "reserva_id")
    UUID reservaId;

    @Column(name = "chave_idempotencia")
    String chaveDeIdempotencia;

    @Column(name = "valor_centavos")
    long valorCentavos;

    @Enumerated(EnumType.STRING)
    StatusPagamento status;

    String motivo;

    @Column(name = "codigo_gateway")
    String codigoGateway;

    @Column(name = "criado_em")
    Instant criadoEm;

    protected PagamentoJpa() {
    }
}