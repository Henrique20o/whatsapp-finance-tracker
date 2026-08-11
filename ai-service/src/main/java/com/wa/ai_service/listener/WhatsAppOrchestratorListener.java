package com.wa.ai_service.listener;

import com.wa.ai_service.config.RabbitMQConfig;
import com.wa.ai_service.dto.MensagemFilaDTO;
import com.wa.ai_service.dto.TransacaoExtraidaDTO;
import com.wa.ai_service.dto.TransacaoRequestDTO;
import com.wa.ai_service.service.LlmCategorizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppOrchestratorListener {

    private final LlmCategorizerService llmCategorizerService;
    private final RabbitTemplate rabbitTemplate; // Ferramenta do Spring para enviar msgs

    @RabbitListener(queues = RabbitMQConfig.WHATSAPP_INPUT_QUEUE)
    public void processarMensagemDoWhatsApp(MensagemFilaDTO mensagemBruta) {

        if ("TEXTO".equalsIgnoreCase(mensagemBruta.tipoMidia())) {
            System.out.println("Processando mensagem de: " + mensagemBruta.telefone());

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

            System.out.println("IA extraiu com sucesso: " + transacaoProcessada.descricao());

            rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSACTION_QUEUE, transacaoComMessageId);
        }
    }
}
