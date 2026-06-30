package io.cinema.mstickets.service;

import io.cinema.mstickets.domain.dto.request.PaymentDTO;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

public interface TicketService {
    Mono<Void> process(@Valid PaymentDTO paymentDTO);
}
