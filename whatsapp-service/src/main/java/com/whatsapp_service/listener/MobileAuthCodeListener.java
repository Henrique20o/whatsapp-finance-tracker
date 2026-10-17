package com.whatsapp_service.listener;

import com.whatsapp_service.config.RabbitMQConfig;
import com.whatsapp_service.security.mobile.MobileAuthDeliveryEnvelope;
import com.whatsapp_service.service.MobileAuthDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MobileAuthCodeListener {

    private final MobileAuthDeliveryService deliveryService;

    @RabbitListener(
            queues = RabbitMQConfig.MOBILE_AUTH_QUEUE,
            containerFactory = "mobileAuthListenerContainerFactory"
    )
    public void processar(MobileAuthDeliveryEnvelope envelope) {
        deliveryService.deliver(envelope);
    }
}
