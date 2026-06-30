package io.cinema.mstickets.service;

import io.cinema.mstickets.config.RabbitMQProperties;
import io.cinema.mstickets.domain.dto.request.NotificationDTO;
import io.cinema.mstickets.domain.dto.request.PaymentDTO;
import io.cinema.mstickets.domain.entity.IdempotencyKeyEntity;
import io.cinema.mstickets.domain.entity.TicketEntity;
import io.cinema.mstickets.domain.enumerated.PaymentStatus;
import io.cinema.mstickets.factory.MockFactory;
import io.cinema.mstickets.mapper.TicketMapper;
import io.cinema.mstickets.repository.IdempotencyRepository;
import io.cinema.mstickets.repository.TicketRepository;
import io.cinema.mstickets.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private TransactionalOperator transactionalOperator;
    @Mock
    private RabbitMQProperties rabbitProperties;
    @Mock
    private RabbitTemplate template;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldFailWhenMessageIsInvalid() {
        // arrange
        var message = new Message(new byte[0]);
        when(objectMapper.readValue(message.getBody(), PaymentDTO.class))
                .thenThrow(new JacksonException("Parse error") {
                });

        // act
        Assertions.assertDoesNotThrow(() -> ticketService.consumeMessage(message));

        // assert
        verifyNoInteractions(idempotencyRepository);
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(template);
    }

    @Test
    void shouldReturnEmptyWhenDatabaseFails() {
        // arrange
        var messageBody = MockFactory.buildPaymentDTO(PaymentStatus.COMPLETED);
        var bodyBytes = MockFactory.buildPaymentDTOBytes(PaymentStatus.COMPLETED);
        var message = new Message(bodyBytes);

        when(objectMapper.readValue(message.getBody(), PaymentDTO.class))
                .thenReturn(messageBody);

        when(idempotencyRepository.findById(messageBody.paymentId()))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("DB Connection Refused")));

        // act
        Assertions.assertDoesNotThrow(() -> ticketService.consumeMessage(message));

        // assert
        verify(idempotencyRepository).findById(messageBody.paymentId());
        verifyNoInteractions(ticketRepository);
        verifyNoInteractions(template);
    }

    @Test
    void shouldIgnoreMessageWhenAlreadyProcessed() {
        // arrange
        var messageBody = MockFactory.buildPaymentDTO(PaymentStatus.COMPLETED);
        var bodyBytes = MockFactory.buildPaymentDTOBytes(PaymentStatus.COMPLETED);
        var message = new Message(bodyBytes);

        when(objectMapper.readValue(message.getBody(), PaymentDTO.class))
                .thenReturn(messageBody);

        var existingKey = new IdempotencyKeyEntity();
        existingKey.setId(messageBody.paymentId());

        when(idempotencyRepository.findById(messageBody.paymentId()))
                .thenReturn(Mono.just(existingKey));

        // act
        Assertions.assertDoesNotThrow(() -> ticketService.consumeMessage(message));

        // assert
        verify(idempotencyRepository).findById(messageBody.paymentId());
        verify(ticketRepository, never()).save(any());
        verify(idempotencyRepository, never()).save(any());
        verifyNoInteractions(template);
    }

    @Test
    void shouldIgnoreMessageWhenStatusIsNotCompleted() {
        // arrange
        var messageBody = MockFactory.buildPaymentDTO(PaymentStatus.PENDING);
        var bodyBytes = MockFactory.buildPaymentDTOBytes(PaymentStatus.PENDING);
        var message = new Message(bodyBytes);
        UUID ticketId = UUID.randomUUID();
        var ticketEntity = MockFactory.buildTicketEntity(ticketId, PaymentStatus.PENDING);
        var idempotencyEntity = MockFactory.buildIdempotencyKey(messageBody.paymentId());

        when(objectMapper.readValue(message.getBody(), PaymentDTO.class)).thenReturn(messageBody);
        when(idempotencyRepository.findById(messageBody.paymentId())).thenReturn(Mono.empty());
        when(ticketMapper.toTicketEntity(messageBody)).thenReturn(ticketEntity);

        when(ticketRepository.save(any(TicketEntity.class))).thenReturn(Mono.just(ticketEntity));
        when(idempotencyRepository.save(any(IdempotencyKeyEntity.class))).thenReturn(Mono.just(idempotencyEntity));

        // act
        Assertions.assertDoesNotThrow(() -> ticketService.consumeMessage(message));

        // assert
        verify(idempotencyRepository).findById(messageBody.paymentId());
        verify(ticketRepository).save(any(TicketEntity.class));
        verify(idempotencyRepository).save(any(IdempotencyKeyEntity.class));
        verifyNoInteractions(template);
        verifyNoInteractions(rabbitProperties);
    }

    @Test
    void shouldProcessMessageAndSendNotificationSuccessfully() {
        // arrange
        var messageBody = MockFactory.buildPaymentDTO(PaymentStatus.COMPLETED);
        var bodyBytes = MockFactory.buildPaymentDTOBytes(PaymentStatus.COMPLETED);
        var message = new Message(bodyBytes);
        UUID ticketId = UUID.randomUUID();
        var ticketEntity = MockFactory.buildTicketEntity(ticketId, PaymentStatus.COMPLETED);
        var idempotencyEntity = MockFactory.buildIdempotencyKey(messageBody.paymentId());
        var notificationProperties = MockFactory.buildQueueProperties();

        when(objectMapper.readValue(message.getBody(), PaymentDTO.class)).thenReturn(messageBody);
        when(idempotencyRepository.findById(messageBody.paymentId())).thenReturn(Mono.empty());
        when(ticketMapper.toTicketEntity(messageBody)).thenReturn(ticketEntity);

        when(ticketRepository.save(any(TicketEntity.class))).thenReturn(Mono.just(ticketEntity));
        when(idempotencyRepository.save(any(IdempotencyKeyEntity.class))).thenReturn(Mono.just(idempotencyEntity));

        when(rabbitProperties.getNotificationProperties()).thenReturn(notificationProperties);
        when(objectMapper.writeValueAsBytes(any(NotificationDTO.class))).thenReturn(new byte[]{1, 2, 3});

        // act
        Assertions.assertDoesNotThrow(() -> ticketService.consumeMessage(message));

        // assert
        verify(idempotencyRepository).findById(messageBody.paymentId());
        verify(ticketRepository).save(any(TicketEntity.class));
        verify(idempotencyRepository).save(any(IdempotencyKeyEntity.class));
        verify(template).send(
                eq(notificationProperties.getExchangeName()),
                eq(notificationProperties.getRoutingKey()),
                any(Message.class)
        );
    }
}