package com.whatsapp_service.config;

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
import com.whatsapp_service.service.TransientMobileAuthDeliveryException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String WHATSAPP_INPUT_QUEUE = "financeiro.v1.whatsapp-entrada";
    public static final String WHATSAPP_OUTPUT_QUEUE = "financeiro.v1.whatsapp-saida";
    public static final String WHATSAPP_OUTPUT_DLQ = "financeiro.v1.whatsapp-saida.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "financeiro.v1.dlx";
    public static final String WHATSAPP_OUTPUT_DLQ_ROUTING_KEY = "financeiro.v1.whatsapp-saida.failed";
    public static final String MOBILE_AUTH_EXCHANGE = "financeiro.v1.whatsapp-auth.exchange";
    public static final String MOBILE_AUTH_ROUTING_KEY = "financeiro.v1.whatsapp-auth.send";
    public static final String MOBILE_AUTH_QUEUE = "financeiro.v1.whatsapp-auth";
    public static final String MOBILE_AUTH_DLQ = "financeiro.v1.whatsapp-auth.dlq";
    public static final String MOBILE_AUTH_DLQ_ROUTING_KEY = "financeiro.v1.whatsapp-auth.failed";
    @Bean
    public Queue whatsappInputQueue() {
        return QueueBuilder
                .durable(WHATSAPP_INPUT_QUEUE)
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
    public Queue whatsappOutputDeadLetterQueue() {
        return QueueBuilder.durable(WHATSAPP_OUTPUT_DLQ).build();
    }

    @Bean
    public Binding whatsappOutputDeadLetterBinding() {
        return BindingBuilder.bind(whatsappOutputDeadLetterQueue())
                .to(deadLetterExchange())
                .with(WHATSAPP_OUTPUT_DLQ_ROUTING_KEY);
    }

    @Bean
    public DirectExchange mobileAuthExchange() {
        return new DirectExchange(MOBILE_AUTH_EXCHANGE, true, false);
    }

    @Bean
    public Queue mobileAuthQueue() {
        return QueueBuilder.durable(MOBILE_AUTH_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MOBILE_AUTH_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding mobileAuthBinding() {
        return BindingBuilder.bind(mobileAuthQueue())
                .to(mobileAuthExchange())
                .with(MOBILE_AUTH_ROUTING_KEY);
    }

    @Bean
    public Queue mobileAuthDeadLetterQueue() {
        return QueueBuilder.durable(MOBILE_AUTH_DLQ).build();
    }

    @Bean
    public Binding mobileAuthDeadLetterBinding() {
        return BindingBuilder.bind(mobileAuthDeadLetterQueue())
                .to(deadLetterExchange())
                .with(MOBILE_AUTH_DLQ_ROUTING_KEY);
    }

    @Bean
    public MethodInterceptor whatsappOutputRetryInterceptor(RabbitTemplate rabbitTemplate) {
        var recoverer = new RepublishMessageRecoverer(
                rabbitTemplate, DEAD_LETTER_EXCHANGE, WHATSAPP_OUTPUT_DLQ_ROUTING_KEY);
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
            MethodInterceptor whatsappOutputRetryInterceptor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(whatsappOutputRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public MethodInterceptor mobileAuthRetryInterceptor(RabbitTemplate rabbitTemplate) {
        var republisher = new RepublishMessageRecoverer(
                rabbitTemplate, DEAD_LETTER_EXCHANGE, MOBILE_AUTH_DLQ_ROUTING_KEY);
        org.springframework.amqp.rabbit.retry.MessageRecoverer recoverer = (message, cause) -> {
            message.getMessageProperties().setExpiration(null);
            republisher.recover(message, cause);
        };
        return RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy
                        .maxRetries(2)
                        .includes(TransientMobileAuthDeliveryException.class))
                .backOffOptions(1_000, 2.0, 2_000)
                .recoverer(recoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory mobileAuthListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MethodInterceptor mobileAuthRetryInterceptor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(mobileAuthRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
