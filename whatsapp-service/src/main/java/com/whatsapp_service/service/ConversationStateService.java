package com.whatsapp_service.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final Map<String, ConversationState> states = new ConcurrentHashMap<>();

    public void aguardarRegistroDeGasto(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_GASTO,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean consumirSeAguardandoGasto(String telefone) {
        ConversationState state = states.remove(telefone);

        return state != null
                && state.step() == ConversationStep.AGUARDANDO_GASTO
                && state.expiresAt().isAfter(Instant.now());
    }

    public void aguardarNomeDaCategoria(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_NOME_CATEGORIA,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean consumirSeAguardandoNomeDaCategoria(String telefone) {
        ConversationState state = states.get(telefone);
        if (state == null) {
            return false;
        }

        if (state.step() != ConversationStep.AGUARDANDO_NOME_CATEGORIA
                || !state.expiresAt().isAfter(Instant.now())) {
            if (!state.expiresAt().isAfter(Instant.now())) {
                states.remove(telefone, state);
            }
            return false;
        }

        states.remove(telefone, state);
        return true;
    }

    public void aguardarCategoriaParaDesativar(String telefone) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_CATEGORIA_PARA_DESATIVAR,
                Instant.now().plus(STATE_TTL),
                null
        ));
    }

    public boolean estaAguardandoCategoriaParaDesativar(String telefone) {
        return consultarPasso(telefone, ConversationStep.AGUARDANDO_CATEGORIA_PARA_DESATIVAR) != null;
    }

    public void aguardarConfirmacaoDeDesativacao(String telefone, String categoria) {
        states.put(telefone, new ConversationState(
                ConversationStep.AGUARDANDO_CONFIRMACAO_DESATIVACAO,
                Instant.now().plus(STATE_TTL),
                categoria
        ));
    }

    public String consumirCategoriaParaConfirmarDesativacao(String telefone) {
        ConversationState state = consumirPasso(
                telefone,
                ConversationStep.AGUARDANDO_CONFIRMACAO_DESATIVACAO
        );
        return state == null ? null : state.contexto();
    }

    public void cancelarFluxo(String telefone) {
        states.remove(telefone);
    }

    private ConversationState consumirPasso(String telefone, ConversationStep passo) {
        ConversationState state = consultarPasso(telefone, passo);
        if (state != null) {
            states.remove(telefone, state);
        }
        return state;
    }

    private ConversationState consultarPasso(String telefone, ConversationStep passo) {
        ConversationState state = states.get(telefone);
        if (state == null || state.step() != passo || !state.expiresAt().isAfter(Instant.now())) {
            if (state != null && !state.expiresAt().isAfter(Instant.now())) {
                states.remove(telefone, state);
            }
            return null;
        }
        return state;
    }

    enum ConversationStep {
        AGUARDANDO_GASTO,
        AGUARDANDO_NOME_CATEGORIA,
        AGUARDANDO_CATEGORIA_PARA_DESATIVAR,
        AGUARDANDO_CONFIRMACAO_DESATIVACAO
    }

    record ConversationState(ConversationStep step, Instant expiresAt, String contexto) {}
}
