package com.wa.finance.outbox;

import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.producer.WhatsAppResponseProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private WhatsAppResponseProducer producer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveMarcarMensagemComoEnviada() {
        OutboxMessage mensagem = mensagemPendente();
        when(repository.findByStatusAndProximaTentativaEmLessThanEqualOrderById(
                any(), any(), any())).thenReturn(List.of(mensagem));

        new OutboxProcessor(repository, producer, objectMapper).processarPendentes();

        verify(producer).enviar(any(RespostaUsuarioDTO.class));
        assertThat(mensagem.getStatus()).isEqualTo(OutboxStatus.ENVIADO);
        assertThat(mensagem.getEnviadoEm()).isNotNull();
    }

    @Test
    void deveManterPendenteEAgendarNovaTentativaQuandoRabbitMqFalhar() {
        OutboxMessage mensagem = mensagemPendente();
        when(repository.findByStatusAndProximaTentativaEmLessThanEqualOrderById(
                any(), any(), any())).thenReturn(List.of(mensagem));
        doThrow(new IllegalStateException("RabbitMQ indisponível"))
                .when(producer).enviar(any(RespostaUsuarioDTO.class));

        new OutboxProcessor(repository, producer, objectMapper).processarPendentes();

        assertThat(mensagem.getStatus()).isEqualTo(OutboxStatus.PENDENTE);
        assertThat(mensagem.getTentativas()).isEqualTo(1);
        assertThat(mensagem.getUltimoErro()).contains("RabbitMQ indisponível");
        assertThat(mensagem.getProximaTentativaEm()).isAfter(LocalDateTime.now());
    }

    private OutboxMessage mensagemPendente() {
        return OutboxMessage.builder()
                .id(1L)
                .tipoEvento(OutboxService.CONFIRMACAO_WHATSAPP)
                .payload("{\"telefone\":\"5531999998888\",\"mensagem\":\"OK\",\"transacaoIdCancelavel\":99}")
                .status(OutboxStatus.PENDENTE)
                .tentativas(0)
                .criadoEm(LocalDateTime.now())
                .proximaTentativaEm(LocalDateTime.now().minusSeconds(1))
                .build();
    }
}
