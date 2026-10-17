package com.wa.finance.outbox;

import com.wa.finance.dto.RespostaUsuarioDTO;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxService {

    public static final String CONFIRMACAO_WHATSAPP = "CONFIRMACAO_WHATSAPP";
    public static final String CODIGO_AUTENTICACAO_WHATSAPP = "CODIGO_AUTENTICACAO_WHATSAPP";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void adicionarConfirmacao(RespostaUsuarioDTO resposta) {
        adicionar(CONFIRMACAO_WHATSAPP, objectMapper.writeValueAsString(resposta));
    }

    public void adicionarCodigoAutenticacao(MobileAuthDeliveryEnvelope envelope) {
        adicionar(CODIGO_AUTENTICACAO_WHATSAPP, objectMapper.writeValueAsString(envelope));
    }

    private void adicionar(String tipoEvento, String payload) {
        OutboxMessage mensagem = OutboxMessage.builder()
                .tipoEvento(tipoEvento)
                .payload(payload)
                .status(OutboxStatus.PENDENTE)
                .tentativas(0)
                .criadoEm(LocalDateTime.now())
                .proximaTentativaEm(LocalDateTime.now())
                .build();
        outboxRepository.save(mensagem);
    }
}
