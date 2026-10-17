package com.wa.finance.service;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreType
public final class DesafioCriado {

    private final UUID challengeId;
    private final String telefoneParaEnvio;
    private final String codigoParaEnvio;
    private final Instant expiraEm;
    private final Instant reenvioPermitidoEm;

    public DesafioCriado(
            UUID challengeId,
            String telefoneParaEnvio,
            String codigoParaEnvio,
            Instant expiraEm,
            Instant reenvioPermitidoEm
    ) {
        this.challengeId = challengeId;
        this.telefoneParaEnvio = telefoneParaEnvio;
        this.codigoParaEnvio = codigoParaEnvio;
        this.expiraEm = expiraEm;
        this.reenvioPermitidoEm = reenvioPermitidoEm;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public String getTelefoneParaEnvio() {
        return telefoneParaEnvio;
    }

    public String getCodigoParaEnvio() {
        return codigoParaEnvio;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getReenvioPermitidoEm() {
        return reenvioPermitidoEm;
    }

    @Override
    public String toString() {
        return "DesafioCriado{" +
                "challengeId=" + challengeId +
                ", expiraEm=" + expiraEm +
                ", reenvioPermitidoEm=" + reenvioPermitidoEm +
                '}';
    }
}
