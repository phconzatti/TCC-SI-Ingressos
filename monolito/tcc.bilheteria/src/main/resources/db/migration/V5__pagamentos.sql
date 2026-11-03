CREATE TABLE pagamentos.reserva_ref (
    id             UUID        PRIMARY KEY,   
    evento_id      UUID        NOT NULL,
    valor_centavos BIGINT      NOT NULL CHECK (valor_centavos >= 0),
    expira_em      TIMESTAMPTZ NOT NULL,
    expirada       BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE pagamentos.pagamento (
    id                 UUID         PRIMARY KEY,
    reserva_id         UUID         NOT NULL REFERENCES pagamentos.reserva_ref (id),
    chave_idempotencia VARCHAR(100) NOT NULL,
    valor_centavos     BIGINT       NOT NULL CHECK (valor_centavos >= 0),
    status             VARCHAR(20)  NOT NULL,
    motivo             VARCHAR(200),
    codigo_gateway     VARCHAR(100),
    criado_em          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pagamento_chave_unica UNIQUE (chave_idempotencia)
);

CREATE UNIQUE INDEX pagamento_ativo_por_reserva_idx
    ON pagamentos.pagamento (reserva_id)
    WHERE status IN ('INICIADO', 'APROVADO');