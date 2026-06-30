package io.cinema.mstickets.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QueueProperties {
    private String queueName;

    private String dlqName;

    private String exchangeName;

    private String dlqExchangeName;

    private String routingKey;

    private String dlqRoutingKey;

    private Integer queueTtl;
}
