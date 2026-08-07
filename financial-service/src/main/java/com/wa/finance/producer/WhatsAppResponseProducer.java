package com.wa.finance.producer;

import com.wa.finance.config.RabbitMQConfig;
import com.wa.finance.dto.RespostaUsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppResponseProducer {

    private final RabbitTemplate rabbitTemplate;

    public void enviar(RespostaUsuarioDTO dto) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WHATSAPP_OUTPUT_QUEUE,
                dto
        );
    }

}