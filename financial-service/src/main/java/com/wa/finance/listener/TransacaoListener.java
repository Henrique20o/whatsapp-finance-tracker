package com.wa.finance.listener;

import com.wa.finance.config.RabbitMQConfig;
import com.wa.finance.dto.TransacaoRequestDTO;
import com.wa.finance.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransacaoListener {

    private final TransacaoService transacaoService;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    public void processarTransacao(TransacaoRequestDTO dto) {
        log.info("Mensagem recebida da fila para processamento financeiro");

        try {
            transacaoService.processarTransacaoDaFila(dto);
            log.info("Transação salva no banco com sucesso via Mensageria!");
        } catch (Exception e) {
            log.error("Erro ao processar transação da fila: {}", e.getMessage());
        }
    }
}
