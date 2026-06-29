package io.cinema.mstickets.config;

import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class RabbitConfig implements RabbitListenerConfigurer {

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        var factory = new DefaultMessageHandlerMethodFactory();

        factory.setValidator(new LocalValidatorFactoryBean());
        factory.afterPropertiesSet();

        registrar.setMessageHandlerMethodFactory(factory);
    }
}
