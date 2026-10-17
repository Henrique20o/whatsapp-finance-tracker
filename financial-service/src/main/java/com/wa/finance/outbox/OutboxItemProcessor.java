package com.wa.finance.outbox;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.producer.MobileAuthDeliveryProducer;
import com.wa.finance.producer.WhatsAppResponseProducer;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxItemProcessor {

    private static final int MAX_AUTH_ATTEMPTS = 5;
    private static final int MAX_BACKOFF_SECONDS = 300;

    private final OutboxRepository outboxRepository;
    private final WhatsAppResponseProducer responseProducer;
    private final MobileAuthDeliveryProducer authDeliveryProducer;
    private final DesafioAutenticacaoRepository desafioRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processarProximo() {
        LocalDateTime agora = LocalDateTime.now(clock);
        var optional = outboxRepository.findNextPendingForUpdate(agora);
        if (optional.isEmpty()) {
            return false;
        }

        OutboxMessage mensagem = optional.get();
        try {
            publicar(mensagem);
            marcarEnviado(mensagem, agora);
        } catch (Exception exception) {
            tratarFalha(mensagem, exception, agora);
        }
        return true;
    }

    private void publicar(OutboxMessage mensagem) {
        if (OutboxService.CONFIRMACAO_WHATSAPP.equals(mensagem.getTipoEvento())) {
            RespostaUsuarioDTO resposta = objectMapper.readValue(
                    mensagem.getPayload(), RespostaUsuarioDTO.class);
            responseProducer.enviar(resposta);
            return;
        }

        if (OutboxService.CODIGO_AUTENTICACAO_WHATSAPP.equals(mensagem.getTipoEvento())) {
            MobileAuthDeliveryEnvelope envelope = objectMapper.readValue(
                    mensagem.getPayload(), MobileAuthDeliveryEnvelope.class);
            if (!envelope.expiresAt().isAfter(clock.instant())) {
                marcarExpirado(mensagem);
                return;
            }
            DesafioAutenticacao desafio = desafioRepository.findById(envelope.challengeId())
                    .orElse(null);
            if (desafio == null || !desafio.estaDisponivel(clock.instant())) {
                marcarExpirado(mensagem);
                return;
            }
            authDeliveryProducer.enviar(envelope);
            return;
        }

        throw new UnsupportedOutboxEventException();
    }

    private void marcarEnviado(OutboxMessage mensagem, LocalDateTime agora) {
        if (mensagem.getStatus() == OutboxStatus.EXPIRADO) {
            return;
        }
        mensagem.setStatus(OutboxStatus.ENVIADO);
        mensagem.setEnviadoEm(agora);
        mensagem.setUltimoErro(null);
    }

    private void marcarExpirado(OutboxMessage mensagem) {
        mensagem.setStatus(OutboxStatus.EXPIRADO);
        mensagem.setUltimoErro("MESSAGE_EXPIRED");
        log.info("event=MOBILE_AUTH_DELIVERY_EXPIRED outboxId={}", mensagem.getId());
    }

    private void tratarFalha(OutboxMessage mensagem, Exception exception, LocalDateTime agora) {
        int tentativas = mensagem.getTentativas() + 1;
        mensagem.setTentativas(tentativas);
        mensagem.setUltimoErro(categoriaSegura(exception));

        boolean autenticacao = OutboxService.CODIGO_AUTENTICACAO_WHATSAPP
                .equals(mensagem.getTipoEvento());
        if (exception instanceof UnsupportedOutboxEventException
                || (autenticacao && tentativas >= MAX_AUTH_ATTEMPTS)) {
            mensagem.setStatus(OutboxStatus.FALHA);
            log.warn("event=OUTBOX_FAILED outboxId={} category={}",
                    mensagem.getId(), mensagem.getUltimoErro());
            return;
        }

        long atraso = Math.min(1L << Math.min(tentativas, 8), MAX_BACKOFF_SECONDS);
        mensagem.setProximaTentativaEm(agora.plusSeconds(atraso));
        log.warn("event=OUTBOX_RETRY outboxId={} attempt={} category={}",
                mensagem.getId(), tentativas, mensagem.getUltimoErro());
    }

    private String categoriaSegura(Exception exception) {
        if (exception instanceof MobileAuthDeliveryProducer.MobileAuthPublishException publishException) {
            return publishException.category();
        }
        if (exception instanceof UnsupportedOutboxEventException) {
            return "UNSUPPORTED_EVENT";
        }
        if (exception.getClass().getName().contains("Jackson")) {
            return "INVALID_PAYLOAD";
        }
        return "PUBLISH_FAILED";
    }

    private static final class UnsupportedOutboxEventException extends RuntimeException {
    }
}
