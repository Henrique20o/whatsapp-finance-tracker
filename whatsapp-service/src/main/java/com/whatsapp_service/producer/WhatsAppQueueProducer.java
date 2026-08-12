package com.whatsapp_service.producer;

import com.whatsapp_service.config.RabbitMQConfig;
import com.whatsapp_service.dto.MensagemFilaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppQueueProducer {

    private final RabbitTemplate rabbitTemplate;

    public void enviarParaProcessamento(MensagemFilaDTO mensagem) {
        log.info("Enviando mensagem para a fila de processamento (Tipo: {})", mensagem.tipoMidia());

        rabbitTemplate.convertAndSend(RabbitMQConfig.WHATSAPP_INPUT_QUEUE, mensagem);
    }
}
