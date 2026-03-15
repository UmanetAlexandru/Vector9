package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private DownstreamEventSelector downstreamEventSelector;
    @Mock
    private DownstreamEventRepository downstreamEventRepository;
    @Mock
    private NotificationChannel notificationChannel;

    @Test
    void shouldDispatchPendingEventsAndRecordFailures() {
        when(downstreamEventSelector.captureOperationalEvents()).thenReturn(2);
        when(downstreamEventRepository.findDeliverableEvents(any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        event(1L, DownstreamEventType.JOB_FAILED, "failure-1"),
                        event(2L, DownstreamEventType.MISSING_STATUS, "missing-2")
                ));
        doNothing()
                .doThrow(new IllegalStateException("telegram unavailable"))
                .when(notificationChannel).send(any(NotificationMessage.class));

        NotificationDispatchService service = new NotificationDispatchService(
                downstreamEventSelector,
                downstreamEventRepository,
                notificationChannel,
                properties(),
                new OperationalMetricsRecorder(new SimpleMeterRegistry()),
                fixedClock()
        );

        NotificationDispatchResult result = service.dispatchOperationalNotifications();

        assertThat(result.eventsCreated()).isEqualTo(2);
        assertThat(result.eventsAttempted()).isEqualTo(2);
        assertThat(result.eventsSent()).isEqualTo(1);
        assertThat(result.eventsFailed()).isEqualTo(1);
        assertThat(result.failureMessages()).hasSize(1);
        verify(downstreamEventRepository).markSent(org.mockito.ArgumentMatchers.eq(1L), any());
        verify(downstreamEventRepository).markFailed(org.mockito.ArgumentMatchers.eq(2L), any(), org.mockito.ArgumentMatchers.contains("telegram unavailable"));
    }

    private DownstreamEvent event(long id, DownstreamEventType type, String messageText) {
        return new DownstreamEvent(
                id,
                "event-" + id,
                type,
                "job-" + id,
                "PROD",
                LocalDateTime.parse("2026-03-15T09:00:00"),
                messageText,
                DownstreamDeliveryStatus.PENDING,
                0,
                null,
                null,
                null,
                LocalDateTime.parse("2026-03-15T09:00:00")
        );
    }

    private DownstreamProperties properties() {
        DownstreamProperties properties = new DownstreamProperties();
        properties.setEnabled(true);
        properties.setBatchSize(10);
        properties.setRetryLimit(3);
        return properties;
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
    }
}
