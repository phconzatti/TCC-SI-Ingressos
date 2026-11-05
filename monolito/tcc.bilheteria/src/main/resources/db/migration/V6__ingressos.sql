CREATE TABLE ingressos.ingresso (
    id           UUID        PRIMARY KEY,
    reserva_id   UUID        NOT NULL,
    numero       INTEGER     NOT NULL CHECK (numero > 0),
    evento_id    UUID        NOT NULL,
    categoria_id UUID        NOT NULL,
    usuario_id   UUID        NOT NULL,
    codigo       VARCHAR(64) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    emitido_em   TIMESTAMPTZ NOT NULL,
    utilizado_em TIMESTAMPTZ,
    CONSTRAINT ingresso_codigo_unico UNIQUE (codigo),
    CONSTRAINT ingresso_reserva_numero_unico UNIQUE (reserva_id, numero)
);