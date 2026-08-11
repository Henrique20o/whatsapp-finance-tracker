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
                Instant.now().plus(STATE_TTL)
        ));
    }

    public boolean consumirSeAguardandoGasto(String telefone) {
        ConversationState state = states.remove(telefone);

        return state != null
                && state.step() == ConversationStep.AGUARDANDO_GASTO
                && state.expiresAt().isAfter(Instant.now());
    }

    enum ConversationStep {
        AGUARDANDO_GASTO
    }

    record ConversationState(ConversationStep step, Instant expiresAt) {}
}
