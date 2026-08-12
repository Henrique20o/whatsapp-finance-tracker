package com.wa.finance.listener;

import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class TransacaoListenerTest {

    @Mock
    private TransacaoService transacaoService;

    @Test
    void devePropagarFalhaParaAcionarRetryDoRabbitMq() {
        var listener = new TransacaoListener(transacaoService);
        var dto = new TransacaoRequestDTO(
                "message-id", "5531999998888", new BigDecimal("50.00"), "Futebol", "Lazer");
        doThrow(new IllegalStateException("Banco indisponível"))
                .when(transacaoService).processarTransacaoDaFila(dto);

        assertThatThrownBy(() -> listener.processarTransacao(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Banco indisponível");
    }
}
