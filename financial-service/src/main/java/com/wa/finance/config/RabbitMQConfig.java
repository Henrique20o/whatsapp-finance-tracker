package com.wa.finance.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_QUEUE = "financeiro.v1.transacoes-entrada";
    public static final String WHATSAPP_OUTPUT_QUEUE = "financeiro.v1.whatsapp-saida";
    public static final String TRANSACTION_DLQ = "financeiro.v1.transacoes-entrada.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "financeiro.v1.dlx";
    public static final String TRANSACTION_DLQ_ROUTING_KEY = "financeiro.v1.transacoes-entrada.failed";

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
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionDeadLetterQueue() {
        return QueueBuilder.durable(TRANSACTION_DLQ).build();
    }

    @Bean
    public Binding transactionDeadLetterBinding() {
        return BindingBuilder.bind(transactionDeadLetterQueue())
                .to(deadLetterExchange())
                .with(TRANSACTION_DLQ_ROUTING_KEY);
    }

    @Bean
    public MethodInterceptor transactionRetryInterceptor(RabbitTemplate rabbitTemplate) {
        var recoverer = new RepublishMessageRecoverer(
                rabbitTemplate, DEAD_LETTER_EXCHANGE, TRANSACTION_DLQ_ROUTING_KEY);
        return RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1_000, 2.0, 5_000)
                .recoverer(recoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MethodInterceptor transactionRetryInterceptor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(transactionRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
