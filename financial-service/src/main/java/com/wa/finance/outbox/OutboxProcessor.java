package com.wa.finance.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int TAMANHO_LOTE = 50;

    private final OutboxItemProcessor itemProcessor;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")
    public void processarPendentes() {
        for (int indice = 0; indice < TAMANHO_LOTE; indice++) {
            if (!itemProcessor.processarProximo()) {
                return;
            }
        }
    }
}
