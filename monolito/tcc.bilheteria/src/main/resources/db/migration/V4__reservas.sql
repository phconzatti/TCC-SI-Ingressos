CREATE TABLE reservas.disponibilidade (
    id             UUID         PRIMARY KEY,   -- é o id da categoria
    evento_id      UUID         NOT NULL,
    nome_categoria VARCHAR(100) NOT NULL,
    preco_centavos BIGINT       NOT NULL CHECK (preco_centavos >= 0),
    capacidade     INTEGER      NOT NULL CHECK (capacidade > 0),
    reservados     INTEGER      NOT NULL DEFAULT 0,
    inicio_vendas  TIMESTAMPTZ  NOT NULL,
    fim_vendas     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT reservados_dentro_da_capacidade CHECK (reservados >= 0 AND reservados <= capacidade)
);
CREATE INDEX disponibilidade_evento_idx ON reservas.disponibilidade (evento_id);

CREATE TABLE reservas.reserva (
    id                   UUID        PRIMARY KEY,
    usuario_id           UUID        NOT NULL,
    evento_id            UUID        NOT NULL,
    categoria_id         UUID        NOT NULL REFERENCES reservas.disponibilidade (id),
    quantidade           INTEGER     NOT NULL CHECK (quantidade > 0),
    valor_total_centavos BIGINT      NOT NULL CHECK (valor_total_centavos >= 0),
    status               VARCHAR(30) NOT NULL,
    criada_em            TIMESTAMPTZ NOT NULL,
    expira_em            TIMESTAMPTZ NOT NULL
);
CREATE INDEX reserva_vencimento_idx ON reservas.reserva (expira_em)
    WHERE status = 'AGUARDANDO_PAGAMENTO';