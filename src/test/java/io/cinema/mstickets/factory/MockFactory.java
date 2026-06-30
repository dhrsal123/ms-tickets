package io.cinema.mstickets.factory;

import io.cinema.mstickets.config.QueueProperties;
import io.cinema.mstickets.domain.dto.request.PaymentDTO;
import io.cinema.mstickets.domain.entity.IdempotencyKeyEntity;
import io.cinema.mstickets.domain.entity.TicketEntity;
import io.cinema.mstickets.domain.enumerated.PaymentStatus;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class MockFactory {
    public static PaymentDTO buildPaymentDTO(PaymentStatus status) {
        return new PaymentDTO(
                UUID.fromString("15dc1c3d-375a-4305-b1cf-cc9f0683301c"),
                LocalDateTime.of(2026, 6, 29, 12, 12, 55),
                "customer@gmail.com",
                UUID.fromString("175dcb06-8b78-4733-8054-57ce60445003"),
                BigDecimal.valueOf(19.99),
                "USD",
                status,
                "Credit Card"
        );
    }

    public static TicketEntity buildTicketEntity(UUID ticketId, PaymentStatus status) {
        return new TicketEntity(
                ticketId,
                UUID.fromString("15dc1c3d-375a-4305-b1cf-cc9f0683301c"),
                "customer@gmail.com",
                "Credit Card",
                "USD",
                BigDecimal.valueOf(19.99),
                LocalDateTime.of(2026, 6, 29, 12, 12, 55),
                status,
                UUID.fromString("175dcb06-8b78-4733-8054-57ce60445003")
        );
    }


    public static byte[] buildPaymentDTOBytes(PaymentStatus status) {
        var statusStr = status.toString();
        var messageString = String.format("""
                {
                  "paymentId": "15dc1c3d-375a-4305-b1cf-cc9f0683301c",
                  "date": "2026-06-29T12:12:55",
                  "customerEmail": "customer@gmail.com",
                  "bookingId": "175dcb06-8b78-4733-8054-57ce60445003",
                  "amount": 19.99,
                  "currency": "USD",
                  "status": "%s",
                  "paymentMethod": "Credit Card"
                }
                """, statusStr);

        return messageString.getBytes(StandardCharsets.UTF_8);
    }

    public static IdempotencyKeyEntity buildIdempotencyKey(UUID paymentId) {
        var idempotencyKey = new IdempotencyKeyEntity();
        idempotencyKey.setId(paymentId);
        idempotencyKey.setResponse("PROCESSED");
        idempotencyKey.setExpiryDate(LocalDateTime.now().plusDays(7));
        idempotencyKey.setNew(true);
        return idempotencyKey;

    }

    public static QueueProperties buildQueueProperties() {
        var queueProperties = new QueueProperties();

        queueProperties.setQueueName("test-queue");
        queueProperties.setDlqName("test-queue.dlq");
        queueProperties.setExchangeName("test-exchange");
        queueProperties.setDlqExchangeName("test-exchange.dx.dlq");
        queueProperties.setRoutingKey("test.*");
        queueProperties.setDlqRoutingKey("dlq-routing");
        queueProperties.setQueueTtl(1000);

        return queueProperties;
    }
}
