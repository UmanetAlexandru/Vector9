package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import md.hashcode.vector9.service.JobExecutionTracker;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatchService {

    private final DownstreamEventSelector downstreamEventSelector;
    private final DownstreamEventRepository downstreamEventRepository;
    private final NotificationChannel notificationChannel;
    private final DownstreamProperties downstreamProperties;
    private final OperationalMetricsRecorder metricsRecorder;
    private final Clock clock;
    private JobExecutionTracker jobExecutionTracker;

    @Autowired
    public NotificationDispatchService(DownstreamEventSelector downstreamEventSelector,
                                       DownstreamEventRepository downstreamEventRepository,
                                       TelegramNotificationChannel notificationChannel,
                                       DownstreamProperties downstreamProperties,
                                       OperationalMetricsRecorder metricsRecorder) {
        this(
                downstreamEventSelector,
                downstreamEventRepository,
                notificationChannel,
                downstreamProperties,
                metricsRecorder,
                Clock.systemDefaultZone()
        );
    }

    NotificationDispatchService(DownstreamEventSelector downstreamEventSelector,
                                DownstreamEventRepository downstreamEventRepository,
                                NotificationChannel notificationChannel,
                                DownstreamProperties downstreamProperties,
                                OperationalMetricsRecorder metricsRecorder,
                                Clock clock) {
        this.downstreamEventSelector = downstreamEventSelector;
        this.downstreamEventRepository = downstreamEventRepository;
        this.notificationChannel = notificationChannel;
        this.downstreamProperties = downstreamProperties;
        this.metricsRecorder = metricsRecorder;
        this.clock = clock;
    }

    public NotificationDispatchResult dispatchOperationalNotifications() {
        if (!downstreamProperties.isEnabled()) {
            return new NotificationDispatchResult(0, 0, 0, 0, List.of());
        }

        long startedAt = System.nanoTime();
        int created = downstreamEventSelector.captureOperationalEvents();
        NotificationDispatchResult result = dispatchPendingEvents(EnumSet.of(
                DownstreamEventType.JOB_FAILED,
                DownstreamEventType.JOB_STALE,
                DownstreamEventType.MISSING_STATUS
        ), created);
        recordExecution(JobExecutionTracker.JOB_DOWNSTREAM_NOTIFICATIONS, startedAt, result);
        return result;
    }

    public NotificationDispatchResult dispatchDailySummaries() {
        if (!downstreamProperties.isEnabled()) {
            return new NotificationDispatchResult(0, 0, 0, 0, List.of());
        }

        long startedAt = System.nanoTime();
        NotificationDispatchResult result = dispatchPendingEvents(EnumSet.of(DownstreamEventType.DAILY_SUMMARY), 0);
        recordExecution(JobExecutionTracker.JOB_DAILY_SUMMARY, startedAt, result);
        return result;
    }

    private NotificationDispatchResult dispatchPendingEvents(Set<DownstreamEventType> eventTypes, int created) {
        List<DownstreamEvent> pendingEvents = downstreamEventRepository.findDeliverableEvents(
                eventTypes,
                downstreamProperties.getRetryLimit(),
                downstreamProperties.getBatchSize()
        );

        int sent = 0;
        int failed = 0;
        List<String> failureMessages = new ArrayList<>();

        for (DownstreamEvent pendingEvent : pendingEvents) {
            LocalDateTime attemptedAt = LocalDateTime.now(clock);
            try {
                notificationChannel.send(new NotificationMessage(pendingEvent.messageText()));
                downstreamEventRepository.markSent(pendingEvent.id(), attemptedAt);
                metricsRecorder.recordDownstreamDelivery(pendingEvent.eventType().name(), true);
                sent++;
            } catch (RuntimeException exception) {
                downstreamEventRepository.markFailed(pendingEvent.id(), attemptedAt, exception.getMessage());
                metricsRecorder.recordDownstreamDelivery(pendingEvent.eventType().name(), false);
                failed++;
                failureMessages.add("%s: %s".formatted(pendingEvent.eventKey(), exception.getMessage()));
            }
        }

        return new NotificationDispatchResult(created, pendingEvents.size(), sent, failed, List.copyOf(failureMessages));
    }

    @Autowired(required = false)
    public void setJobExecutionTracker(JobExecutionTracker jobExecutionTracker) {
        this.jobExecutionTracker = jobExecutionTracker;
    }

    private void recordExecution(String jobName, long startedAt, NotificationDispatchResult result) {
        if (jobExecutionTracker == null) {
            return;
        }

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        if (result.eventsFailed() == 0) {
            jobExecutionTracker.recordSuccess(jobName, durationMs);
        } else {
            jobExecutionTracker.recordFailure(jobName, durationMs, String.join("; ", result.failureMessages()));
        }
    }
}
