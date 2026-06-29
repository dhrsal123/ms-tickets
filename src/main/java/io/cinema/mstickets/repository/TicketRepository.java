package io.cinema.mstickets.repository;

import io.cinema.mstickets.domain.entity.TicketEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends ReactiveCrudRepository<TicketEntity, UUID> {
}
