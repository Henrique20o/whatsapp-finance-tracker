package com.whatsapp_service.listener;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.dto.RespostaFinanceiroDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WhatsAppResponseListenerTest {

    @Mock
    private WuzApiClient wuzApiClient;

    @InjectMocks
    private WhatsAppResponseListener listener;

    @Test
    void deveEnviarConfirmacaoComBotaoQuandoTransacaoForCancelavel() {
        RespostaFinanceiroDTO resposta = new RespostaFinanceiroDTO(
                "5531999998888",
                "Gasto registrado",
                99L
        );

        listener.processarReposta(resposta);

        verify(wuzApiClient).enviarConfirmacaoComCancelamento(
                "5531999998888",
                "Gasto registrado",
                99L
        );
        verify(wuzApiClient, never()).enviarMensagem(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void deveManterEnvioDeTextoParaRespostaSemTransacao() {
        RespostaFinanceiroDTO resposta = new RespostaFinanceiroDTO(
                "5531999998888",
                "Mensagem informativa",
                null
        );

        listener.processarReposta(resposta);

        verify(wuzApiClient).enviarMensagem("5531999998888", "Mensagem informativa");
        verify(wuzApiClient, never()).enviarConfirmacaoComCancelamento(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    void devePropagarFalhaParaAcionarRetryDoRabbitMq() {
        RespostaFinanceiroDTO resposta = new RespostaFinanceiroDTO(
                "5531999998888", "Mensagem informativa", null);
        doThrow(new IllegalStateException("WuzAPI indisponível"))
                .when(wuzApiClient).enviarMensagem(resposta.telefone(), resposta.mensagem());

        assertThatThrownBy(() -> listener.processarReposta(resposta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("WuzAPI indisponível");
    }
}
