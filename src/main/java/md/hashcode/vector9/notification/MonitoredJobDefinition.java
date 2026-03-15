package md.hashcode.vector9.notification;

public record MonitoredJobDefinition(
        String jobName,
        String displayName,
        long freshnessThresholdMinutes
) {
}
