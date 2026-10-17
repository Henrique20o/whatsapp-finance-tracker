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
@Table(name = "tb_desafio_autenticacao")
public class DesafioAutenticacao {

    @Id
    private UUID id;

    @Column(name = "telefone_criptografado", nullable = false, length = 512)
    private String telefoneCriptografado;

    @Column(name = "telefone_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String telefoneHash;

    @Column(name = "codigo_hmac", nullable = false, length = 64, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String codigoHmac;

    @Column(name = "ip_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String ipHash;

    @Column(nullable = false)
    private short tentativas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "reenvio_permitido_em", nullable = false)
    private Instant reenvioPermitidoEm;

    @Column(name = "consumido_em")
    private Instant consumidoEm;

    @Column(name = "invalidado_em")
    private Instant invalidadoEm;

    protected DesafioAutenticacao() {
    }

    private DesafioAutenticacao(
            UUID id,
            String telefoneCriptografado,
            String telefoneHash,
            String codigoHmac,
            String ipHash,
            Instant criadoEm,
            Instant expiraEm,
            Instant reenvioPermitidoEm
    ) {
        this.id = Objects.requireNonNull(id);
        this.telefoneCriptografado = Objects.requireNonNull(telefoneCriptografado);
        this.telefoneHash = Objects.requireNonNull(telefoneHash);
        this.codigoHmac = Objects.requireNonNull(codigoHmac);
        this.ipHash = Objects.requireNonNull(ipHash);
        this.criadoEm = Objects.requireNonNull(criadoEm);
        this.expiraEm = Objects.requireNonNull(expiraEm);
        this.reenvioPermitidoEm = Objects.requireNonNull(reenvioPermitidoEm);
        this.tentativas = 0;

        if (!expiraEm.isAfter(criadoEm)
                || !reenvioPermitidoEm.isAfter(criadoEm)
                || reenvioPermitidoEm.isAfter(expiraEm)) {
            throw new IllegalArgumentException("Períodos do desafio são inválidos");
        }
    }

    public static DesafioAutenticacao criar(
            UUID id,
            String telefoneCriptografado,
            String telefoneHash,
            String codigoHmac,
            String ipHash,
            Instant criadoEm,
            Instant expiraEm,
            Instant reenvioPermitidoEm
    ) {
        return new DesafioAutenticacao(
                id,
                telefoneCriptografado,
                telefoneHash,
                codigoHmac,
                ipHash,
                criadoEm,
                expiraEm,
                reenvioPermitidoEm
        );
    }

    public boolean estaExpirado(Instant agora) {
        return !Objects.requireNonNull(agora).isBefore(expiraEm);
    }

    public boolean foiFinalizado() {
        return consumidoEm != null || invalidadoEm != null;
    }

    public boolean estaDisponivel(Instant agora) {
        return !foiFinalizado() && !estaExpirado(agora);
    }

    public void invalidar(Instant agora) {
        Objects.requireNonNull(agora);
        if (consumidoEm == null && invalidadoEm == null) {
            if (agora.isBefore(criadoEm)) {
                throw new IllegalArgumentException("Invalidação anterior à criação");
            }
            invalidadoEm = agora;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTelefoneCriptografado() {
        return telefoneCriptografado;
    }

    public String getTelefoneHash() {
        return telefoneHash;
    }

    public String getCodigoHmac() {
        return codigoHmac;
    }

    public String getIpHash() {
        return ipHash;
    }

    public short getTentativas() {
        return tentativas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getReenvioPermitidoEm() {
        return reenvioPermitidoEm;
    }

    public Instant getConsumidoEm() {
        return consumidoEm;
    }

    public Instant getInvalidadoEm() {
        return invalidadoEm;
    }

    @Override
    public String toString() {
        return "DesafioAutenticacao{" +
                "id=" + id +
                ", tentativas=" + tentativas +
                ", criadoEm=" + criadoEm +
                ", expiraEm=" + expiraEm +
                ", finalizado=" + foiFinalizado() +
                '}';
    }
}
