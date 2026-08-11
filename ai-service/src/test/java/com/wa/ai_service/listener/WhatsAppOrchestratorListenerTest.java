package com.wa.ai_service.listener;

import com.wa.ai_service.config.RabbitMQConfig;
import com.wa.ai_service.dto.MensagemFilaDTO;
import com.wa.ai_service.dto.TransacaoExtraidaDTO;
import com.wa.ai_service.dto.TransacaoRequestDTO;
import com.wa.ai_service.service.LlmCategorizerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppOrchestratorListenerTest {

    @Mock
    private LlmCategorizerService llmCategorizerService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WhatsAppOrchestratorListener listener;

    @Test
    void devePropagarMessageIdETransacaoExtraidaParaFilaFinanceira() {
        MensagemFilaDTO mensagem = new MensagemFilaDTO(
                "message-123",
                "5531999998888",
                "TEXTO",
                "Gastei 50 reais no futebol"
        );
        TransacaoExtraidaDTO extraida = new TransacaoExtraidaDTO(
                mensagem.telefone(),
                new BigDecimal("50.00"),
                "Jogo de futebol",
                "Lazer"
        );
        when(llmCategorizerService.extrairTransacao(mensagem.telefone(), mensagem.conteudo()))
                .thenReturn(extraida);

        listener.processarMensagemDoWhatsApp(mensagem);

        ArgumentCaptor<TransacaoRequestDTO> transacao = ArgumentCaptor.forClass(TransacaoRequestDTO.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TRANSACTION_QUEUE),
                transacao.capture()
        );
        assertThat(transacao.getValue()).isEqualTo(new TransacaoRequestDTO(
                "message-123",
                "5531999998888",
                new BigDecimal("50.00"),
                "Jogo de futebol",
                "Lazer"
        ));
    }

    @Test
    void deveIgnorarTipoDeMidiaAindaNaoSuportado() {
        MensagemFilaDTO mensagem = new MensagemFilaDTO(
                "message-audio",
                "5531999998888",
                "AUDIO",
                "https://exemplo/audio.ogg"
        );

        listener.processarMensagemDoWhatsApp(mensagem);

        verify(llmCategorizerService, never()).extrairTransacao(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(rabbitTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(TransacaoRequestDTO.class)
        );
    }
}
