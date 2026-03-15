package md.hashcode.vector9.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import md.hashcode.vector9.service.JobExecutionSnapshot;
import org.springframework.stereotype.Component;

@Component
public class NotificationFormatter {

    public String formatJobFailed(String environmentName,
                                  MonitoredJobDefinition jobDefinition,
                                  JobExecutionSnapshot snapshot) {
        String error = snapshot.lastError() == null || snapshot.lastError().isBlank()
                ? "No error details recorded"
                : snapshot.lastError();

        return """
                %s Job failed
                Job: %s
                Last failure: %s
                Error: %s
                """.formatted(prefix(environmentName), jobDefinition.displayName(), snapshot.lastFailureAt(), error).trim();
    }

    public String formatJobStale(String environmentName,
                                 MonitoredJobDefinition jobDefinition,
                                 JobExecutionSnapshot snapshot) {
        return """
                %s Job stale
                Job: %s
                Last success: %s
                Freshness threshold: %d minutes
                """.formatted(
                prefix(environmentName),
                jobDefinition.displayName(),
                snapshot.lastSuccessAt(),
                jobDefinition.freshnessThresholdMinutes()
        ).trim();
    }

    public String formatMissingStatus(String environmentName, MonitoredJobDefinition jobDefinition) {
        return """
                %s Missing job status
                Job: %s
                State: no execution snapshot found
                """.formatted(prefix(environmentName), jobDefinition.displayName()).trim();
    }

    public String describeJobStatus(MonitoredJobDefinition jobDefinition,
                                    Optional<JobExecutionSnapshot> snapshot,
                                    LocalDateTime now) {
        if (snapshot.isEmpty()) {
            return "- %s: missing".formatted(jobDefinition.displayName());
        }

        JobExecutionSnapshot value = snapshot.get();
        if (value.lastFailureAt() != null
                && (value.lastSuccessAt() == null || value.lastFailureAt().isAfter(value.lastSuccessAt()))) {
            return "- %s: failed at %s".formatted(jobDefinition.displayName(), value.lastFailureAt());
        }

        if (value.lastSuccessAt() == null) {
            return "- %s: missing".formatted(jobDefinition.displayName());
        }

        if (value.lastSuccessAt().isBefore(now.minusMinutes(jobDefinition.freshnessThresholdMinutes()))) {
            return "- %s: stale, last success %s".formatted(jobDefinition.displayName(), value.lastSuccessAt());
        }

        return "- %s: fresh, last success %s".formatted(jobDefinition.displayName(), value.lastSuccessAt());
    }

    public String formatDailySummary(String environmentName,
                                     LocalDate summaryDate,
                                     List<String> jobStatusLines,
                                     Map<DownstreamEventType, Long> eventCounts,
                                     long activeAds,
                                     long deletedAds,
                                     long enrichedAds) {
        return """
                %s Daily summary for %s
                Current job status:
                %s

                Alert events created:
                - JOB_FAILED: %d
                - JOB_STALE: %d
                - MISSING_STATUS: %d

                Ad totals:
                - active: %d
                - deleted: %d
                - enriched: %d
                """.formatted(
                prefix(environmentName),
                summaryDate,
                String.join(System.lineSeparator(), jobStatusLines),
                eventCounts.getOrDefault(DownstreamEventType.JOB_FAILED, 0L),
                eventCounts.getOrDefault(DownstreamEventType.JOB_STALE, 0L),
                eventCounts.getOrDefault(DownstreamEventType.MISSING_STATUS, 0L),
                activeAds,
                deletedAds,
                enrichedAds
        ).trim();
    }

    private String prefix(String environmentName) {
        return "[" + environmentName.toUpperCase(Locale.ROOT) + "]";
    }
}
