package com.wa.finance.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_QUEUE = "financeiro.v1.transacoes";
    public static final String WHATSAPP_OUTPUT_QUEUE = "financeiro.v1.whatsapp-saida";

    @Bean
    public Queue transactionQueue() {
        return QueueBuilder
                .durable(TRANSACTION_QUEUE)
                .build();
    }

    @Bean
    public Queue whatsappOutputQueue() {
        return QueueBuilder
                .durable(WHATSAPP_OUTPUT_QUEUE)
                .build();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}