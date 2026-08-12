package com.whatsapp_service.listener;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.config.RabbitMQConfig;
import com.whatsapp_service.dto.RespostaFinanceiroDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppResponseListener {

    private final WuzApiClient wuzapiClient;

    @RabbitListener(queues = RabbitMQConfig.WHATSAPP_OUTPUT_QUEUE)
    public void processarReposta(RespostaFinanceiroDTO resposta) {
        log.info("Mensagem financeira recebida para envio pelo WhatsApp");

        if (resposta.transacaoIdCancelavel() != null) {
            wuzapiClient.enviarConfirmacaoComCancelamento(
                    resposta.telefone(),
                    resposta.mensagem(),
                    resposta.transacaoIdCancelavel()
            );
        } else {
            wuzapiClient.enviarMensagem(resposta.telefone(), resposta.mensagem());
        }

        log.info("Mensagem enviada com sucesso pelo WuzAPI");
    }
}
