package com.wa.finance.outbox;

import com.wa.finance.dto.RespostaUsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxService {

    public static final String CONFIRMACAO_WHATSAPP = "CONFIRMACAO_WHATSAPP";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void adicionarConfirmacao(RespostaUsuarioDTO resposta) {
        OutboxMessage mensagem = OutboxMessage.builder()
                .tipoEvento(CONFIRMACAO_WHATSAPP)
                .payload(objectMapper.writeValueAsString(resposta))
                .status(OutboxStatus.PENDENTE)
                .tentativas(0)
                .criadoEm(LocalDateTime.now())
                .proximaTentativaEm(LocalDateTime.now())
                .build();
        outboxRepository.save(mensagem);
    }
}
