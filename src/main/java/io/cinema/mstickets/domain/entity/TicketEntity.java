package io.cinema.mstickets.domain.entity;

import io.cinema.domain.entity.AuditableEntity;
import io.cinema.mstickets.domain.enumerated.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket")
public class TicketEntity extends AuditableEntity {
    @Id
    private UUID id;

    private UUID paymentId;
    private String customerEmail;

    private String paymentMethod;
    private String currency;

    private BigDecimal amount;

    private LocalDateTime paymentDate;

    private PaymentStatus status;

    private UUID bookingId;

}
