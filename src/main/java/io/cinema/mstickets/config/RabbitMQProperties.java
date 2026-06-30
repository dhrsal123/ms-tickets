package io.cinema.mstickets.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@NoArgsConstructor
@ConfigurationProperties(prefix = "tickets.rabbitmq")
public class RabbitMQProperties {
    private QueueProperties paymentProperties;
    private QueueProperties notificationProperties;

}
