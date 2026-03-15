package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {

    private final DownstreamEventRepository downstreamEventRepository;
    private final JobExecutionStateRepository jobExecutionStateRepository;
    private final AdRepository adRepository;
    private final MonitoredJobCatalog monitoredJobCatalog;
    private final NotificationFormatter notificationFormatter;
    private final DownstreamProperties downstreamProperties;
    private final OperationalMetricsRecorder metricsRecorder;
    private final Clock clock;

    @Autowired
    public ReportGenerationService(DownstreamEventRepository downstreamEventRepository,
                                   JobExecutionStateRepository jobExecutionStateRepository,
                                   AdRepository adRepository,
                                   MonitoredJobCatalog monitoredJobCatalog,
                                   NotificationFormatter notificationFormatter,
                                   DownstreamProperties downstreamProperties,
                                   OperationalMetricsRecorder metricsRecorder) {
        this(
                downstreamEventRepository,
                jobExecutionStateRepository,
                adRepository,
                monitoredJobCatalog,
                notificationFormatter,
                downstreamProperties,
                metricsRecorder,
                Clock.systemDefaultZone()
        );
    }

    ReportGenerationService(DownstreamEventRepository downstreamEventRepository,
                            JobExecutionStateRepository jobExecutionStateRepository,
                            AdRepository adRepository,
                            MonitoredJobCatalog monitoredJobCatalog,
                            NotificationFormatter notificationFormatter,
                            DownstreamProperties downstreamProperties,
                            OperationalMetricsRecorder metricsRecorder,
                            Clock clock) {
        this.downstreamEventRepository = downstreamEventRepository;
        this.jobExecutionStateRepository = jobExecutionStateRepository;
        this.adRepository = adRepository;
        this.monitoredJobCatalog = monitoredJobCatalog;
        this.notificationFormatter = notificationFormatter;
        this.downstreamProperties = downstreamProperties;
        this.metricsRecorder = metricsRecorder;
        this.clock = clock;
    }

    public boolean queueDailySummaryForPreviousDay() {
        if (!downstreamProperties.isEnabled() || !downstreamProperties.isDailySummaryEnabled()) {
            return false;
        }

        LocalDate summaryDate = LocalDate.now(clock).minusDays(1);
        LocalDateTime windowStart = summaryDate.atStartOfDay();
        LocalDateTime windowEnd = summaryDate.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(clock);

        List<String> jobStatusLines = monitoredJobCatalog.expectedJobs().stream()
                .map(definition -> notificationFormatter.describeJobStatus(
                        definition,
                        jobExecutionStateRepository.findByJobName(definition.jobName()),
                        now
                ))
                .toList();

        String messageText = notificationFormatter.formatDailySummary(
                downstreamProperties.getEnvironmentName(),
                summaryDate,
                jobStatusLines,
                downstreamEventRepository.countEventsCreatedBetween(windowStart, windowEnd),
                adRepository.countByStatus("active"),
                adRepository.countByStatus("deleted"),
                adRepository.countEnrichedAds()
        );

        boolean created = downstreamEventRepository.createPendingEvent(
                "daily-summary:%s".formatted(summaryDate),
                DownstreamEventType.DAILY_SUMMARY,
                null,
                downstreamProperties.getEnvironmentName(),
                windowEnd.minusSeconds(1),
                messageText
        );
        if (created) {
            metricsRecorder.recordDownstreamEventCreated(DownstreamEventType.DAILY_SUMMARY.name());
        }
        return created;
    }
}
