package io.cinema.mstickets.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PaymentQueueConfigs {
    private final RabbitMQProperties rabbitMQProperties;

    @Bean
    public TopicExchange paymentTopicExchange() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new TopicExchange(properties.getExchangeName());
    }

    @Bean
    public Queue paymentQueue() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(properties.getQueueTtl())
                .deadLetterExchange(properties.getDlqExchangeName())
                .deadLetterRoutingKey(properties.getDlqRoutingKey())
                .build();
    }

    @Bean
    public Binding paymentBinding(TopicExchange paymentTopicExchange, Queue paymentQueue) {
        var properties = rabbitMQProperties.getPaymentProperties();
        return BindingBuilder
                .bind(paymentQueue)
                .to(paymentTopicExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public DirectExchange paymentDlqExchange() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new DirectExchange(properties.getDlqExchangeName());
    }

    @Bean
    public Queue paymentDeadLettersQueue() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new Queue(properties.getDlqName());
    }

    @Bean
    public Binding paymentDeadLettersBinding(DirectExchange paymentDlqExchange, Queue paymentDeadLettersQueue) {
        var properties = rabbitMQProperties.getPaymentProperties();
        return BindingBuilder
                .bind(paymentDeadLettersQueue)
                .to(paymentDlqExchange)
                .with(properties.getDlqRoutingKey());
    }
}