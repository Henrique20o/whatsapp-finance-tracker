package com.wa.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tb_sessao_mobile")
public class SessaoMobile {

    @Id
    private UUID id;

    @Column(name = "sessao_id", nullable = false)
    private UUID sessaoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "refresh_token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String refreshTokenHash;

    @Column(name = "token_anterior_id")
    private UUID tokenAnteriorId;

    @Column(name = "nome_dispositivo", length = 120)
    private String nomeDispositivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_inatividade_em", nullable = false)
    private Instant expiraInatividadeEm;

    @Column(name = "expira_absoluto_em", nullable = false)
    private Instant expiraAbsolutoEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "motivo_revogacao", length = 40)
    private String motivoRevogacao;

    protected SessaoMobile() {
    }

    public static SessaoMobile criar(
            UUID id, UUID sessaoId, Long usuarioId, String refreshTokenHash, UUID tokenAnteriorId,
            String nomeDispositivo, Instant criadoEm, Instant expiraInatividadeEm, Instant expiraAbsolutoEm
    ) {
        SessaoMobile sessao = new SessaoMobile();
        sessao.id = Objects.requireNonNull(id);
        sessao.sessaoId = Objects.requireNonNull(sessaoId);
        sessao.usuarioId = Objects.requireNonNull(usuarioId);
        sessao.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        sessao.tokenAnteriorId = tokenAnteriorId;
        sessao.nomeDispositivo = nomeDispositivo;
        sessao.criadoEm = Objects.requireNonNull(criadoEm);
        sessao.expiraInatividadeEm = Objects.requireNonNull(expiraInatividadeEm);
        sessao.expiraAbsolutoEm = Objects.requireNonNull(expiraAbsolutoEm);
        if (!expiraInatividadeEm.isAfter(criadoEm) || expiraAbsolutoEm.isBefore(expiraInatividadeEm)) {
            throw new IllegalArgumentException("Prazos da sessão mobile inválidos");
        }
        return sessao;
    }

    public boolean estaAtiva(Instant agora) {
        return usadoEm == null && revogadoEm == null
                && agora.isBefore(expiraInatividadeEm) && agora.isBefore(expiraAbsolutoEm);
    }

    public void marcarUsada(Instant agora) {
        if (usadoEm != null || revogadoEm != null) throw new IllegalStateException("Token já finalizado");
        usadoEm = Objects.requireNonNull(agora);
    }

    public void revogar(Instant agora, String motivo) {
        if (revogadoEm == null) {
            revogadoEm = Objects.requireNonNull(agora);
            motivoRevogacao = Objects.requireNonNull(motivo);
        }
    }

    public UUID getId() { return id; }
    public UUID getSessaoId() { return sessaoId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public UUID getTokenAnteriorId() { return tokenAnteriorId; }
    public String getNomeDispositivo() { return nomeDispositivo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExpiraInatividadeEm() { return expiraInatividadeEm; }
    public Instant getExpiraAbsolutoEm() { return expiraAbsolutoEm; }
    public Instant getUsadoEm() { return usadoEm; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public String getMotivoRevogacao() { return motivoRevogacao; }
}
