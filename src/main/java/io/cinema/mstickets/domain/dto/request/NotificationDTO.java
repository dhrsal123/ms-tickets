package io.cinema.mstickets.domain.dto.request;

import io.cinema.mstickets.domain.enumerated.NotificationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record NotificationDTO(
        @NotNull(message = "Notification ID is required")
        UUID notificationId,

        @NotBlank(message = "Notification type cannot be blank")
        String notificationType,

        @NotBlank(message = "Subject cannot be blank")
        String subject,

        @NotNull(message = "Provider is required")
        NotificationProvider provider,

        @NotBlank(message = "Recipient cannot be blank")
        String recipient,

        @NotNull(message = "Template model cannot be null")
        Map<String, Object> templateModel

) {
}
