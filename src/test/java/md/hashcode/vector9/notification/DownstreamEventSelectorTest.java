package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionSnapshot;
import md.hashcode.vector9.service.ObservabilityProperties;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownstreamEventSelectorTest {

    @Mock
    private JobExecutionStateRepository jobExecutionStateRepository;
    @Mock
    private DownstreamEventRepository downstreamEventRepository;

    @Test
    void shouldCreateFailureAndMissingStatusEvents() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
        when(jobExecutionStateRepository.findByJobName("incremental-crawler"))
                .thenReturn(Optional.of(new JobExecutionSnapshot(
                        "incremental-crawler",
                        LocalDateTime.parse("2026-03-15T08:00:00"),
                        LocalDateTime.parse("2026-03-15T09:00:00"),
                        1200L,
                        "timeout",
                        LocalDateTime.parse("2026-03-15T09:00:00")
                )));
        when(jobExecutionStateRepository.findByJobName("deletion-detection")).thenReturn(Optional.empty());
        when(jobExecutionStateRepository.findByJobName("view-tracking"))
                .thenReturn(Optional.of(new JobExecutionSnapshot(
                        "view-tracking",
                        LocalDateTime.parse("2026-03-15T09:50:00"),
                        null,
                        500L,
                        null,
                        LocalDateTime.parse("2026-03-15T09:50:00")
                )));
        when(jobExecutionStateRepository.findByJobName("detail-enrichment"))
                .thenReturn(Optional.of(new JobExecutionSnapshot(
                        "detail-enrichment",
                        LocalDateTime.parse("2026-03-14T08:00:00"),
                        null,
                        800L,
                        null,
                        LocalDateTime.parse("2026-03-14T08:00:00")
                )));
        when(downstreamEventRepository.createPendingEvent(anyString(), any(), any(), anyString(), any(), anyString()))
                .thenReturn(true);

        DownstreamEventSelector selector = new DownstreamEventSelector(
                catalog(),
                jobExecutionStateRepository,
                downstreamEventRepository,
                new NotificationFormatter(),
                properties(),
                new OperationalMetricsRecorder(new SimpleMeterRegistry()),
                clock
        );

        int created = selector.captureOperationalEvents();

        assertThat(created).isEqualTo(3);
        verify(downstreamEventRepository).createPendingEvent(
                org.mockito.ArgumentMatchers.contains("job-failed:incremental-crawler"),
                org.mockito.ArgumentMatchers.eq(DownstreamEventType.JOB_FAILED),
                org.mockito.ArgumentMatchers.eq("incremental-crawler"),
                org.mockito.ArgumentMatchers.eq("PROD"),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.parse("2026-03-15T09:00:00")),
                org.mockito.ArgumentMatchers.contains("Job failed")
        );
        verify(downstreamEventRepository).createPendingEvent(
                org.mockito.ArgumentMatchers.eq("missing-status:deletion-detection"),
                org.mockito.ArgumentMatchers.eq(DownstreamEventType.MISSING_STATUS),
                org.mockito.ArgumentMatchers.eq("deletion-detection"),
                org.mockito.ArgumentMatchers.eq("PROD"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.contains("Missing job status")
        );
        verify(downstreamEventRepository).createPendingEvent(
                org.mockito.ArgumentMatchers.contains("job-stale:detail-enrichment"),
                org.mockito.ArgumentMatchers.eq(DownstreamEventType.JOB_STALE),
                org.mockito.ArgumentMatchers.eq("detail-enrichment"),
                org.mockito.ArgumentMatchers.eq("PROD"),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.parse("2026-03-14T08:00:00")),
                org.mockito.ArgumentMatchers.contains("Job stale")
        );
    }

    private MonitoredJobCatalog catalog() {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.setIncrementalCrawlerFreshnessMinutes(30);
        properties.setDeletionDetectionFreshnessMinutes(180);
        properties.setViewTrackingFreshnessMinutes(180);
        properties.setDetailEnrichmentFreshnessMinutes(1440);
        return new MonitoredJobCatalog(properties);
    }

    private DownstreamProperties properties() {
        DownstreamProperties properties = new DownstreamProperties();
        properties.setEnvironmentName("PROD");
        return properties;
    }
}
