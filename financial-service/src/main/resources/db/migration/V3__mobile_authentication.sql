CREATE TABLE tb_desafio_autenticacao (
    id UUID NOT NULL,
    telefone_criptografado VARCHAR(512) NOT NULL,
    telefone_hash CHAR(64) NOT NULL,
    codigo_hmac CHAR(64) NOT NULL,
    ip_hash CHAR(64) NOT NULL,
    tentativas SMALLINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMPTZ NOT NULL,
    reenvio_permitido_em TIMESTAMPTZ NOT NULL,
    consumido_em TIMESTAMPTZ,
    invalidado_em TIMESTAMPTZ,
    CONSTRAINT pk_desafio_autenticacao PRIMARY KEY (id),
    CONSTRAINT ck_desafio_auth_tentativas
        CHECK (tentativas BETWEEN 0 AND 5),
    CONSTRAINT ck_desafio_auth_expiracao
        CHECK (expira_em > criado_em),
    CONSTRAINT ck_desafio_auth_reenvio
        CHECK (reenvio_permitido_em > criado_em AND reenvio_permitido_em <= expira_em),
    CONSTRAINT ck_desafio_auth_finalizacao
        CHECK (NOT (consumido_em IS NOT NULL AND invalidado_em IS NOT NULL)),
    CONSTRAINT ck_desafio_auth_consumido_data
        CHECK (consumido_em IS NULL OR consumido_em >= criado_em),
    CONSTRAINT ck_desafio_auth_invalidado_data
        CHECK (invalidado_em IS NULL OR invalidado_em >= criado_em)
);

CREATE UNIQUE INDEX uk_desafio_auth_telefone_ativo
    ON tb_desafio_autenticacao (telefone_hash)
    WHERE consumido_em IS NULL AND invalidado_em IS NULL;

CREATE INDEX idx_desafio_auth_telefone_criado
    ON tb_desafio_autenticacao (telefone_hash, criado_em DESC);

CREATE INDEX idx_desafio_auth_ip_criado
    ON tb_desafio_autenticacao (ip_hash, criado_em DESC);

CREATE INDEX idx_desafio_auth_expiracao
    ON tb_desafio_autenticacao (expira_em);

CREATE TABLE tb_sessao_mobile (
    id UUID NOT NULL,
    sessao_id UUID NOT NULL,
    usuario_id BIGINT NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    token_anterior_id UUID,
    nome_dispositivo VARCHAR(120),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_inatividade_em TIMESTAMPTZ NOT NULL,
    expira_absoluto_em TIMESTAMPTZ NOT NULL,
    usado_em TIMESTAMPTZ,
    revogado_em TIMESTAMPTZ,
    motivo_revogacao VARCHAR(40),
    CONSTRAINT pk_sessao_mobile PRIMARY KEY (id),
    CONSTRAINT uk_sessao_mobile_identidade_familia
        UNIQUE (id, sessao_id, usuario_id),
    CONSTRAINT uk_sessao_mobile_refresh_hash
        UNIQUE (refresh_token_hash),
    CONSTRAINT uk_sessao_mobile_token_anterior
        UNIQUE (token_anterior_id),
    CONSTRAINT fk_sessao_mobile_usuario
        FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_sessao_mobile_token_anterior
        FOREIGN KEY (token_anterior_id, sessao_id, usuario_id)
        REFERENCES tb_sessao_mobile (id, sessao_id, usuario_id),
    CONSTRAINT ck_sessao_mobile_expiracao_inatividade
        CHECK (expira_inatividade_em > criado_em),
    CONSTRAINT ck_sessao_mobile_expiracao_absoluta
        CHECK (expira_absoluto_em >= expira_inatividade_em),
    CONSTRAINT ck_sessao_mobile_usado_data
        CHECK (usado_em IS NULL OR usado_em >= criado_em),
    CONSTRAINT ck_sessao_mobile_revogado_data
        CHECK (revogado_em IS NULL OR revogado_em >= criado_em),
    CONSTRAINT ck_sessao_mobile_revogacao
        CHECK (
            (revogado_em IS NULL AND motivo_revogacao IS NULL)
            OR (revogado_em IS NOT NULL AND motivo_revogacao IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uk_sessao_mobile_familia_ativa
    ON tb_sessao_mobile (sessao_id)
    WHERE usado_em IS NULL AND revogado_em IS NULL;

CREATE INDEX idx_sessao_mobile_familia
    ON tb_sessao_mobile (sessao_id);

CREATE INDEX idx_sessao_mobile_usuario_ativa
    ON tb_sessao_mobile (usuario_id)
    WHERE usado_em IS NULL AND revogado_em IS NULL;

CREATE INDEX idx_sessao_mobile_expiracao
    ON tb_sessao_mobile (expira_inatividade_em, expira_absoluto_em);
