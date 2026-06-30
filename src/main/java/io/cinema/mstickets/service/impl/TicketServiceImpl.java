package io.cinema.mstickets.service.impl;

import io.cinema.domain.exceptions.CinemaException;
import io.cinema.mstickets.config.RabbitMQProperties;
import io.cinema.mstickets.domain.dto.request.NotificationDTO;
import io.cinema.mstickets.domain.dto.request.PaymentDTO;
import io.cinema.mstickets.domain.entity.IdempotencyKeyEntity;
import io.cinema.mstickets.domain.enumerated.PaymentStatus;
import io.cinema.mstickets.mapper.TicketMapper;
import io.cinema.mstickets.repository.IdempotencyRepository;
import io.cinema.mstickets.repository.TicketRepository;
import io.cinema.mstickets.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.cinema.domain.enumerated.CinemaExceptionTypes.TECHNICAL_ERROR;
import static io.cinema.mstickets.domain.enumerated.NotificationProvider.GOOGLE;


@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransactionalOperator transactionalOperator;
    private final RabbitMQProperties rabbitProperties;

    private final RabbitTemplate template;
    private final ObjectMapper objectMapper;


    @RabbitListener(queues = "${tickets.rabbitmq.payment-properties.queueName}")
    public void consumeMessage(Message message) {
        try {
            PaymentDTO paymentDTO = objectMapper.readValue(
                    message.getBody(),
                    PaymentDTO.class
            );

            process(paymentDTO)
                    .onErrorResume(e -> {
                        log.error("Error processing payment {}: {}", paymentDTO.paymentId(), e.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to deserialize message", e);
        }
    }

    @Override
    public Mono<Void> process(@Valid PaymentDTO paymentDTO) {
        return idempotencyRepository.findById(paymentDTO.paymentId())
                .flatMap(existingKey -> {
                    log.info("Payment {} already processed. Ignoring duplicate.", existingKey.getId());
                    return Mono.just(existingKey);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    var ticketEntity = ticketMapper.toTicketEntity(paymentDTO);

                    var idempotencyKey = new IdempotencyKeyEntity();
                    idempotencyKey.setId(paymentDTO.paymentId());
                    idempotencyKey.setResponse("PROCESSED");
                    idempotencyKey.setExpiryDate(LocalDateTime.now().plusDays(7));
                    idempotencyKey.setNew(true);

                    return ticketRepository.save(ticketEntity)
                            .then(idempotencyRepository.save(idempotencyKey))
                            .flatMap(savedKey ->
                                    sendNotificationIfNeeded(paymentDTO).thenReturn(savedKey)
                            );
                }))
                .as(transactionalOperator::transactional)
                .doOnError(e -> log.error("DB error during ticket processing: {}", e.getMessage()))
                .onErrorMap(
                        e -> !(e instanceof CinemaException),
                        e -> new CinemaException("DB error during read", TECHNICAL_ERROR)
                )
                .then();
    }

    private Mono<Void> sendNotificationIfNeeded(PaymentDTO paymentDTO) {
        if (paymentDTO.status() != PaymentStatus.COMPLETED) {
            return Mono.empty();
        }

        return Mono.defer(() -> {
            try {
                var properties = rabbitProperties.getNotificationProperties();
                // TODO: FETCH FROM THE BOOKING SERVICE THE PRODUCT NAME
                var templateVariables = Map.<String, Object>of(
                        "paymentId", paymentDTO.paymentId(),
                        "paymentDate", paymentDTO.date(),
                        "productName", "TEMPORARY TEST",
                        "amount", paymentDTO.amount(),
                        "currencyType", paymentDTO.currency(),
                        "status", paymentDTO.status(),
                        "paymentMethod", paymentDTO.paymentMethod()
                );

                NotificationDTO notification = new NotificationDTO(
                        UUID.randomUUID(),
                        "PAYMENT",
                        "Payment confirmation.",
                        GOOGLE,
                        paymentDTO.customerEmail(),
                        templateVariables
                );

                byte[] messageBody = objectMapper.writeValueAsBytes(notification);

                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                Message message = new Message(messageBody, messageProperties);

                return Mono.fromRunnable(() -> template.send(
                        properties.getExchangeName(),
                        properties.getRoutingKey(),
                        message
                )).subscribeOn(Schedulers.boundedElastic());
            } catch (Exception e) {
                return Mono.error(new CinemaException("Error compiling notification payload", TECHNICAL_ERROR));
            }
        }).then();
    }

}
