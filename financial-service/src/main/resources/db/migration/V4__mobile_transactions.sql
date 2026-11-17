ALTER TABLE tb_transacao
    ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP';

ALTER TABLE tb_transacao
    ADD CONSTRAINT ck_transacao_origem CHECK (origem IN ('WHATSAPP', 'MOBILE'));

CREATE TABLE tb_idempotencia_mobile (
    id UUID NOT NULL,
    usuario_id BIGINT NOT NULL,
    operacao VARCHAR(40) NOT NULL,
    chave_hash CHAR(64) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    transacao_id BIGINT,
    resposta_json TEXT,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_idempotencia_mobile PRIMARY KEY (id),
    CONSTRAINT uk_idempotencia_mobile_chave UNIQUE (usuario_id, operacao, chave_hash),
    CONSTRAINT fk_idempotencia_mobile_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_idempotencia_mobile_transacao FOREIGN KEY (transacao_id) REFERENCES tb_transacao (id)
);

CREATE INDEX idx_idempotencia_mobile_criada_em ON tb_idempotencia_mobile (criada_em);
