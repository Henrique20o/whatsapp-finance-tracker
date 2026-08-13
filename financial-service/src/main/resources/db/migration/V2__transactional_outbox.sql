CREATE TABLE tb_outbox (
    id BIGSERIAL PRIMARY KEY,
    tipo_evento VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_em TIMESTAMP,
    ultimo_erro VARCHAR(500)
);

CREATE INDEX idx_outbox_pendente
    ON tb_outbox (status, proxima_tentativa_em, id);
