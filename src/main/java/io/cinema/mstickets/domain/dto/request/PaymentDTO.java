package io.cinema.mstickets.domain.dto.request;

import io.cinema.mstickets.domain.enumerated.PaymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDTO(
        @NotNull(message = "Payment ID cannot be null")
        UUID paymentId,

        @NotNull(message = "Payment date cannot be null")
        @PastOrPresent(message = "Payment date cannot be in the future")
        LocalDateTime date,

        @NotBlank(message = "Customer email cannot be blank")
        @Email(message = "Must be a valid email format")
        String customerEmail,

        @NotNull(message = "Booking ID cannot be null")
        UUID bookingId,

        @NotNull(message = "Amount cannot be null")
        @Positive(message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency cannot be blank")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Payment status cannot be null")
        PaymentStatus status,

        @NotBlank(message = "Payment method cannot be blank")
        String paymentMethod
) {
}
