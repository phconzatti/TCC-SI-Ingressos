CREATE TABLE eventos.evento (
    id              UUID         PRIMARY KEY,
    nome            VARCHAR(200) NOT NULL,
    local_do_evento VARCHAR(200) NOT NULL,
    data_do_evento  TIMESTAMPTZ  NOT NULL,
    capacidade      INTEGER      NOT NULL CHECK (capacidade > 0),
    status          VARCHAR(20)  NOT NULL
);

CREATE TABLE eventos.categoria (
    id             UUID         PRIMARY KEY,
    evento_id      UUID         NOT NULL REFERENCES eventos.evento (id),
    nome           VARCHAR(100) NOT NULL,
    preco_centavos BIGINT       NOT NULL CHECK (preco_centavos >= 0),
    capacidade     INTEGER      NOT NULL CHECK (capacidade > 0),
    inicio_vendas  TIMESTAMPTZ  NOT NULL,
    fim_vendas     TIMESTAMPTZ  NOT NULL,
    UNIQUE (evento_id, nome)
);