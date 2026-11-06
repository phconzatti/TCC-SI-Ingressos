CREATE TABLE usuarios.usuario (
    id         UUID         PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    papel      VARCHAR(20)  NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT usuario_email_unico UNIQUE (email)
);