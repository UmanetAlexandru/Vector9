package md.hashcode.vector9.notification;

import java.time.LocalDateTime;

public record DownstreamEvent(
        long id,
        String eventKey,
        DownstreamEventType eventType,
        String jobName,
        String environmentName,
        LocalDateTime eventAt,
        String messageText,
        DownstreamDeliveryStatus status,
        int attemptCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime deliveredAt,
        String lastError,
        LocalDateTime createdAt
) {
}
