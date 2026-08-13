package com.wa.ai_service.listener;

import com.wa.ai_service.config.RabbitMQConfig;
import com.wa.ai_service.dto.MensagemFilaDTO;
import com.wa.ai_service.dto.TransacaoExtraidaDTO;
import com.wa.ai_service.dto.TransacaoRequestDTO;
import com.wa.ai_service.service.LlmCategorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppOrchestratorListener {

    private final LlmCategorizerService llmCategorizerService;
    private final RabbitTemplate rabbitTemplate; // Ferramenta do Spring para enviar msgs

    @RabbitListener(queues = RabbitMQConfig.WHATSAPP_INPUT_QUEUE)
    public void processarMensagemDoWhatsApp(MensagemFilaDTO mensagemBruta) {

        if ("TEXTO".equalsIgnoreCase(mensagemBruta.tipoMidia())) {
            log.info("Processando mensagem financeira recebida: messageId={}", mensagemBruta.messageId());

            TransacaoExtraidaDTO transacaoProcessada = llmCategorizerService.extrairTransacao(
                    mensagemBruta.telefone(),
                    mensagemBruta.conteudo()
            );

            TransacaoRequestDTO transacaoComMessageId = new TransacaoRequestDTO(
                    mensagemBruta.messageId(),
                    mensagemBruta.telefone(),
                    transacaoProcessada.valor(),
                    transacaoProcessada.descricao(),
                    transacaoProcessada.categoriaNome()
            );

            log.info("Extração concluída: messageId={}, categoria={}",
                    mensagemBruta.messageId(), transacaoProcessada.categoriaNome());

            rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSACTION_QUEUE, transacaoComMessageId);
        }
    }
}
