package com.wa.finance.outbox;

import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.producer.WhatsAppResponseProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int TAMANHO_LOTE = 50;
    private static final int BACKOFF_MAXIMO_SEGUNDOS = 300;

    private final OutboxRepository outboxRepository;
    private final WhatsAppResponseProducer responseProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")
    @Transactional
    public void processarPendentes() {
        var pendentes = outboxRepository
                .findByStatusAndProximaTentativaEmLessThanEqualOrderById(
                        OutboxStatus.PENDENTE,
                        LocalDateTime.now(),
                        PageRequest.of(0, TAMANHO_LOTE)
                );

        for (OutboxMessage mensagem : pendentes) {
            try {
                if (!OutboxService.CONFIRMACAO_WHATSAPP.equals(mensagem.getTipoEvento())) {
                    throw new IllegalStateException("Tipo de evento outbox não suportado");
                }
                RespostaUsuarioDTO resposta = objectMapper.readValue(
                        mensagem.getPayload(), RespostaUsuarioDTO.class);
                responseProducer.enviar(resposta);
                mensagem.setStatus(OutboxStatus.ENVIADO);
                mensagem.setEnviadoEm(LocalDateTime.now());
                mensagem.setUltimoErro(null);
            } catch (Exception exception) {
                int tentativas = mensagem.getTentativas() + 1;
                mensagem.setTentativas(tentativas);
                mensagem.setUltimoErro(resumirErro(exception));
                long atraso = Math.min(1L << Math.min(tentativas, 8), BACKOFF_MAXIMO_SEGUNDOS);
                mensagem.setProximaTentativaEm(LocalDateTime.now().plusSeconds(atraso));
                log.warn("Falha ao publicar mensagem outbox id={}; nova tentativa agendada", mensagem.getId());
            }
        }
    }

    private String resumirErro(Exception exception) {
        String mensagem = exception.getMessage();
        if (mensagem == null) {
            return exception.getClass().getSimpleName();
        }
        return mensagem.substring(0, Math.min(mensagem.length(), 500));
    }
}
