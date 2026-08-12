CREATE TABLE IF NOT EXISTS tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100),
    email VARCHAR(100),
    telefone VARCHAR(255) NOT NULL,
    telefone_hash VARCHAR(64),
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tb_usuario ALTER COLUMN telefone TYPE VARCHAR(255);
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS telefone_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_telefone_hash
    ON tb_usuario (telefone_hash) WHERE telefone_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS tb_categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_categoria_usuario_ativa ON tb_categoria (usuario_id, ativa);

CREATE TABLE IF NOT EXISTS tb_transacao (
    id BIGSERIAL PRIMARY KEY,
    external_message_id VARCHAR(255),
    valor NUMERIC(15, 2) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletado BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    categoria_id BIGINT NOT NULL REFERENCES tb_categoria(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_transacao_external_message_id
    ON tb_transacao (external_message_id) WHERE external_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transacao_usuario_data ON tb_transacao (usuario_id, data_hora);
