package md.hashcode.vector9.config;

import java.time.Clock;
import java.time.LocalDateTime;

import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionSnapshot;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class JobFreshnessHealthIndicator implements HealthIndicator {

    private final JobExecutionStateRepository repository;
    private final Clock clock;
    private final String jobName;
    private final String displayName;
    private final long freshnessThresholdMinutes;

    public JobFreshnessHealthIndicator(JobExecutionStateRepository repository,
                                       Clock clock,
                                       String jobName,
                                       String displayName,
                                       long freshnessThresholdMinutes) {
        this.repository = repository;
        this.clock = clock;
        this.jobName = jobName;
        this.displayName = displayName;
        this.freshnessThresholdMinutes = freshnessThresholdMinutes;
    }

    @Override
    public Health health() {
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            return repository.findByJobName(jobName)
                    .map(snapshot -> toHealth(snapshot, now))
                    .orElseGet(() -> Health.up()
                            .withDetail("job", displayName)
                            .withDetail("freshness", "not_yet_run")
                            .withDetail("thresholdMinutes", freshnessThresholdMinutes)
                            .build());
        } catch (RuntimeException exception) {
            return Health.up()
                    .withDetail("job", displayName)
                    .withDetail("freshness", "unavailable")
                    .withDetail("thresholdMinutes", freshnessThresholdMinutes)
                    .withDetail("error", exception.getMessage())
                    .build();
        }
    }

    private Health toHealth(JobExecutionSnapshot snapshot, LocalDateTime now) {
        LocalDateTime staleCutoff = now.minusMinutes(freshnessThresholdMinutes);
        if (snapshot.lastFailureAt() != null
                && (snapshot.lastSuccessAt() == null || snapshot.lastFailureAt().isAfter(snapshot.lastSuccessAt()))) {
            return Health.down()
                    .withDetail("job", displayName)
                    .withDetail("lastFailureAt", snapshot.lastFailureAt())
                    .withDetail("lastError", snapshot.lastError())
                    .withDetail("thresholdMinutes", freshnessThresholdMinutes)
                    .build();
        }

        if (snapshot.lastSuccessAt() != null && snapshot.lastSuccessAt().isBefore(staleCutoff)) {
            return Health.down()
                    .withDetail("job", displayName)
                    .withDetail("freshness", "stale")
                    .withDetail("lastSuccessAt", snapshot.lastSuccessAt())
                    .withDetail("thresholdMinutes", freshnessThresholdMinutes)
                    .build();
        }

        return Health.up()
                .withDetail("job", displayName)
                .withDetail("freshness", snapshot.lastSuccessAt() == null ? "not_yet_run" : "fresh")
                .withDetail("lastSuccessAt", snapshot.lastSuccessAt())
                .withDetail("lastDurationMs", snapshot.lastDurationMs())
                .withDetail("thresholdMinutes", freshnessThresholdMinutes)
                .build();
    }
}
