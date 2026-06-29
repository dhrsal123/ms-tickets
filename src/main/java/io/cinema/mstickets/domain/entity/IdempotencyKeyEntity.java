package io.cinema.mstickets.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "idempotency_key")
public class IdempotencyKeyEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    private String response;
    private LocalDateTime expiryDate;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
