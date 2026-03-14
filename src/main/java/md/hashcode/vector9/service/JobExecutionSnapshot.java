package md.hashcode.vector9.service;

import java.time.LocalDateTime;

public record JobExecutionSnapshot(
        String jobName,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt,
        Long lastDurationMs,
        String lastError,
        LocalDateTime lastUpdatedAt
) {
}
