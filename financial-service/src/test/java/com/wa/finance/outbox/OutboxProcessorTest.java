package com.wa.finance.outbox;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxProcessorTest {

    @Test
    void deveProcessarAteNaoExistirMaisMensagemPendente() {
        OutboxItemProcessor itemProcessor = mock(OutboxItemProcessor.class);
        when(itemProcessor.processarProximo()).thenReturn(true, true, false);

        new OutboxProcessor(itemProcessor).processarPendentes();

        verify(itemProcessor, times(3)).processarProximo();
    }

    @Test
    void deveLimitarCadaLoteACinquentaItens() {
        OutboxItemProcessor itemProcessor = mock(OutboxItemProcessor.class);
        when(itemProcessor.processarProximo()).thenReturn(true);

        new OutboxProcessor(itemProcessor).processarPendentes();

        verify(itemProcessor, times(50)).processarProximo();
    }
}
