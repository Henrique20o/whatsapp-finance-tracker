package com.wa.finance.producer;

import com.wa.finance.config.RabbitMQConfig;
import com.wa.finance.security.mobile.delivery.MobileAuthDeliveryEnvelope;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class MobileAuthDeliveryProducer {

    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public MobileAuthDeliveryProducer(RabbitTemplate rabbitTemplate, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    public void enviar(MobileAuthDeliveryEnvelope envelope) {
        long ttlMillis = Duration.between(clock.instant(), envelope.expiresAt()).toMillis();
        if (ttlMillis <= 0) {
            throw new MobileAuthPublishException("MESSAGE_EXPIRED");
        }

        CorrelationData correlationData = new CorrelationData(envelope.messageId().toString());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MOBILE_AUTH_EXCHANGE,
                RabbitMQConfig.MOBILE_AUTH_ROUTING_KEY,
                envelope,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setExpiration(Long.toString(ttlMillis));
                    return message;
                },
                correlationData
        );

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack()) {
                throw new MobileAuthPublishException("BROKER_NACK");
            }
            if (correlationData.getReturned() != null) {
                throw new MobileAuthPublishException("BROKER_RETURNED");
            }
        } catch (MobileAuthPublishException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MobileAuthPublishException("BROKER_CONFIRM_INTERRUPTED", exception);
        } catch (Exception exception) {
            throw new MobileAuthPublishException("BROKER_CONFIRM_FAILED", exception);
        }
    }

    public static final class MobileAuthPublishException extends RuntimeException {
        private final String category;

        public MobileAuthPublishException(String category) {
            super(category);
            this.category = category;
        }

        public MobileAuthPublishException(String category, Throwable cause) {
            super(category, cause);
            this.category = category;
        }

        public String category() {
            return category;
        }
    }
}
