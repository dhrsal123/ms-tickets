package io.cinema.mstickets.repository;

import io.cinema.mstickets.domain.entity.IdempotencyKeyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdempotencyRepository extends ReactiveCrudRepository<IdempotencyKeyEntity, UUID> {
}
