package io.cinema.mstickets.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationQueueConfigs {
    private final RabbitMQProperties rabbitMQProperties;


    @Bean
    public TopicExchange notificationTopicExchange() {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();
        return new TopicExchange(notificationProperties.getExchangeName());
    }

    @Bean
    public Queue notificationsQueue() {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();
        return QueueBuilder.durable(notificationProperties.getQueueName())
                .ttl(notificationProperties.getQueueTtl())
                .deadLetterExchange(notificationProperties.getDlqExchangeName())
                .deadLetterRoutingKey(notificationProperties.getDlqRoutingKey())
                .build();
    }

    @Bean
    public Binding notificationsBinding(
            TopicExchange notificationTopicExchange,
            Queue notificationsQueue
    ) {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();

        return BindingBuilder
                .bind(notificationsQueue)
                .to(notificationTopicExchange)
                .with(notificationProperties.getRoutingKey());
    }

    @Bean
    public DirectExchange directExchange() {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();
        return new DirectExchange(notificationProperties.getDlqExchangeName());
    }

    @Bean
    public Queue deadLettersQueue() {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();
        return new Queue(notificationProperties.getDlqName());
    }

    @Bean
    public Binding deadLettersBinding(
            DirectExchange directExchange,
            Queue deadLettersQueue
    ) {
        var notificationProperties = rabbitMQProperties.getNotificationProperties();
        return BindingBuilder
                .bind(deadLettersQueue)
                .to(directExchange)
                .with(notificationProperties.getDlqRoutingKey());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
