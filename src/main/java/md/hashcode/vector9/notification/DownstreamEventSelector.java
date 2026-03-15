package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.LocalDateTime;

import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionSnapshot;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DownstreamEventSelector {

    private final MonitoredJobCatalog monitoredJobCatalog;
    private final JobExecutionStateRepository jobExecutionStateRepository;
    private final DownstreamEventRepository downstreamEventRepository;
    private final NotificationFormatter notificationFormatter;
    private final DownstreamProperties downstreamProperties;
    private final OperationalMetricsRecorder metricsRecorder;
    private final Clock clock;

    @Autowired
    public DownstreamEventSelector(MonitoredJobCatalog monitoredJobCatalog,
                                   JobExecutionStateRepository jobExecutionStateRepository,
                                   DownstreamEventRepository downstreamEventRepository,
                                   NotificationFormatter notificationFormatter,
                                   DownstreamProperties downstreamProperties,
                                   OperationalMetricsRecorder metricsRecorder) {
        this(
                monitoredJobCatalog,
                jobExecutionStateRepository,
                downstreamEventRepository,
                notificationFormatter,
                downstreamProperties,
                metricsRecorder,
                Clock.systemDefaultZone()
        );
    }

    DownstreamEventSelector(MonitoredJobCatalog monitoredJobCatalog,
                            JobExecutionStateRepository jobExecutionStateRepository,
                            DownstreamEventRepository downstreamEventRepository,
                            NotificationFormatter notificationFormatter,
                            DownstreamProperties downstreamProperties,
                            OperationalMetricsRecorder metricsRecorder,
                            Clock clock) {
        this.monitoredJobCatalog = monitoredJobCatalog;
        this.jobExecutionStateRepository = jobExecutionStateRepository;
        this.downstreamEventRepository = downstreamEventRepository;
        this.notificationFormatter = notificationFormatter;
        this.downstreamProperties = downstreamProperties;
        this.metricsRecorder = metricsRecorder;
        this.clock = clock;
    }

    public int captureOperationalEvents() {
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;

        for (MonitoredJobDefinition jobDefinition : monitoredJobCatalog.expectedJobs()) {
            var snapshot = jobExecutionStateRepository.findByJobName(jobDefinition.jobName());
            if (snapshot.isEmpty()) {
                if (createEvent(
                        missingStatusKey(jobDefinition),
                        DownstreamEventType.MISSING_STATUS,
                        jobDefinition.jobName(),
                        now,
                        notificationFormatter.formatMissingStatus(downstreamProperties.getEnvironmentName(), jobDefinition)
                )) {
                    created++;
                }
                continue;
            }

            JobExecutionSnapshot current = snapshot.get();
            if (current.lastFailureAt() != null
                    && (current.lastSuccessAt() == null || current.lastFailureAt().isAfter(current.lastSuccessAt()))) {
                if (createEvent(
                        failureKey(jobDefinition, current),
                        DownstreamEventType.JOB_FAILED,
                        jobDefinition.jobName(),
                        current.lastFailureAt(),
                        notificationFormatter.formatJobFailed(downstreamProperties.getEnvironmentName(), jobDefinition, current)
                )) {
                    created++;
                }
                continue;
            }

            if (current.lastSuccessAt() != null
                    && current.lastSuccessAt().isBefore(now.minusMinutes(jobDefinition.freshnessThresholdMinutes()))) {
                if (createEvent(
                        staleKey(jobDefinition, current),
                        DownstreamEventType.JOB_STALE,
                        jobDefinition.jobName(),
                        current.lastSuccessAt(),
                        notificationFormatter.formatJobStale(downstreamProperties.getEnvironmentName(), jobDefinition, current)
                )) {
                    created++;
                }
            }
        }

        return created;
    }

    private boolean createEvent(String eventKey,
                                DownstreamEventType eventType,
                                String jobName,
                                LocalDateTime eventAt,
                                String messageText) {
        boolean created = downstreamEventRepository.createPendingEvent(
                eventKey,
                eventType,
                jobName,
                downstreamProperties.getEnvironmentName(),
                eventAt,
                messageText
        );
        if (created) {
            metricsRecorder.recordDownstreamEventCreated(eventType.name());
        }
        return created;
    }

    private String failureKey(MonitoredJobDefinition jobDefinition, JobExecutionSnapshot snapshot) {
        return "job-failed:%s:%s".formatted(jobDefinition.jobName(), snapshot.lastFailureAt());
    }

    private String staleKey(MonitoredJobDefinition jobDefinition, JobExecutionSnapshot snapshot) {
        return "job-stale:%s:%s".formatted(jobDefinition.jobName(), snapshot.lastSuccessAt());
    }

    private String missingStatusKey(MonitoredJobDefinition jobDefinition) {
        return "missing-status:%s".formatted(jobDefinition.jobName());
    }
}
