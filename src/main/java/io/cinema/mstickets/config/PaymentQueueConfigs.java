package io.cinema.mstickets.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PaymentQueueConfigs {
    private final RabbitMQProperties rabbitMQProperties;

    @Bean
    public FanoutExchange paymentSuccessExchange() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new FanoutExchange(properties.getExchangeName());
    }

    @Bean
    public Queue ticketEventQueue() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(properties.getQueueTtl())
                .deadLetterExchange(properties.getDlqExchangeName())
                .deadLetterRoutingKey(properties.getDlqRoutingKey())
                .build();
    }

    @Bean
    public Binding ticketEventBinding(FanoutExchange paymentSuccessExchange, Queue ticketEventQueue) {
        return BindingBuilder
                .bind(ticketEventQueue)
                .to(paymentSuccessExchange);
    }

    @Bean
    public DirectExchange ticketDlqExchange() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new DirectExchange(properties.getDlqExchangeName());
    }

    @Bean
    public Queue ticketDeadLettersQueue() {
        var properties = rabbitMQProperties.getPaymentProperties();
        return new Queue(properties.getDlqName());
    }

    @Bean
    public Binding ticketDeadLettersBinding(DirectExchange ticketDlqExchange, Queue ticketDeadLettersQueue) {
        var properties = rabbitMQProperties.getPaymentProperties();
        return BindingBuilder
                .bind(ticketDeadLettersQueue)
                .to(ticketDlqExchange)
                .with(properties.getDlqRoutingKey());
    }
}