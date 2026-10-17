package com.wa.finance.outbox;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.producer.MobileAuthDeliveryProducer;
import com.wa.finance.producer.WhatsAppResponseProducer;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxItemProcessorTest {

    private final Instant now = Instant.parse("2026-10-17T20:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final WhatsAppResponseProducer responseProducer = mock(WhatsAppResponseProducer.class);
    private final MobileAuthDeliveryProducer authProducer = mock(MobileAuthDeliveryProducer.class);
    private final DesafioAutenticacaoRepository desafioRepository = mock(DesafioAutenticacaoRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OutboxItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxItemProcessor(
                repository, responseProducer, authProducer, desafioRepository, objectMapper, clock);
    }

    @Test
    void devePublicarCodigoValidoEMarcarEnviado() {
        MobileAuthDeliveryEnvelope envelope = envelope(now.plusSeconds(300));
        OutboxMessage message = authMessage(envelope, 0);
        DesafioAutenticacao challenge = mock(DesafioAutenticacao.class);
        when(repository.findNextPendingForUpdate(LocalDateTime.now(clock)))
                .thenReturn(Optional.of(message));
        when(desafioRepository.findById(envelope.challengeId())).thenReturn(Optional.of(challenge));
        when(challenge.estaDisponivel(now)).thenReturn(true);

        assertThat(processor.processarProximo()).isTrue();

        verify(authProducer).enviar(envelope);
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.ENVIADO);
        assertThat(message.getEnviadoEm()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void deveDescartarCodigoExpiradoSemPublicar() {
        MobileAuthDeliveryEnvelope envelope = envelope(now);
        OutboxMessage message = authMessage(envelope, 0);
        when(repository.findNextPendingForUpdate(LocalDateTime.now(clock)))
                .thenReturn(Optional.of(message));

        processor.processarProximo();

        verify(authProducer, never()).enviar(any());
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.EXPIRADO);
        assertThat(message.getUltimoErro()).isEqualTo("MESSAGE_EXPIRED");
    }

    @Test
    void deveEncerrarAposQuintaFalhaSemPersistirMensagemDaExcecao() {
        MobileAuthDeliveryEnvelope envelope = envelope(now.plusSeconds(300));
        OutboxMessage message = authMessage(envelope, 4);
        DesafioAutenticacao challenge = mock(DesafioAutenticacao.class);
        when(repository.findNextPendingForUpdate(LocalDateTime.now(clock)))
                .thenReturn(Optional.of(message));
        when(desafioRepository.findById(envelope.challengeId())).thenReturn(Optional.of(challenge));
        when(challenge.estaDisponivel(now)).thenReturn(true);
        doThrow(new MobileAuthDeliveryProducer.MobileAuthPublishException("BROKER_NACK"))
                .when(authProducer).enviar(envelope);

        processor.processarProximo();

        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FALHA);
        assertThat(message.getTentativas()).isEqualTo(5);
        assertThat(message.getUltimoErro()).isEqualTo("BROKER_NACK");
    }

    @Test
    void devePreservarPublicacaoFinanceiraExistente() {
        OutboxMessage message = OutboxMessage.builder()
                .id(2L)
                .tipoEvento(OutboxService.CONFIRMACAO_WHATSAPP)
                .payload("{\"telefone\":\"5531999998888\",\"mensagem\":\"OK\",\"transacaoIdCancelavel\":99}")
                .status(OutboxStatus.PENDENTE)
                .tentativas(0)
                .criadoEm(LocalDateTime.now(clock))
                .proximaTentativaEm(LocalDateTime.now(clock))
                .build();
        when(repository.findNextPendingForUpdate(LocalDateTime.now(clock)))
                .thenReturn(Optional.of(message));

        processor.processarProximo();

        verify(responseProducer).enviar(any(RespostaUsuarioDTO.class));
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.ENVIADO);
    }

    private MobileAuthDeliveryEnvelope envelope(Instant expiresAt) {
        return new MobileAuthDeliveryEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), expiresAt,
                "v1", "aXY=", "Y2lwaGVydGV4dA==");
    }

    private OutboxMessage authMessage(MobileAuthDeliveryEnvelope envelope, int attempts) {
        return OutboxMessage.builder()
                .id(1L)
                .tipoEvento(OutboxService.CODIGO_AUTENTICACAO_WHATSAPP)
                .payload(objectMapper.writeValueAsString(envelope))
                .status(OutboxStatus.PENDENTE)
                .tentativas(attempts)
                .criadoEm(LocalDateTime.now(clock))
                .proximaTentativaEm(LocalDateTime.now(clock))
                .build();
    }
}
