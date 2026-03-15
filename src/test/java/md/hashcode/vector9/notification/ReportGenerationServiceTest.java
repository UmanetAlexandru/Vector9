package md.hashcode.vector9.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionSnapshot;
import md.hashcode.vector9.service.ObservabilityProperties;
import md.hashcode.vector9.service.OperationalMetricsRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock
    private DownstreamEventRepository downstreamEventRepository;
    @Mock
    private JobExecutionStateRepository jobExecutionStateRepository;
    @Mock
    private AdRepository adRepository;

    @Test
    void shouldQueuePreviousDaySummaryOnce() {
        when(jobExecutionStateRepository.findByJobName(anyString()))
                .thenReturn(Optional.of(new JobExecutionSnapshot(
                        "job",
                        LocalDateTime.parse("2026-03-15T07:00:00"),
                        null,
                        1000L,
                        null,
                        LocalDateTime.parse("2026-03-15T07:00:00")
                )));
        when(downstreamEventRepository.countEventsCreatedBetween(any(), any()))
                .thenReturn(Map.of(
                        DownstreamEventType.JOB_FAILED, 2L,
                        DownstreamEventType.JOB_STALE, 1L,
                        DownstreamEventType.MISSING_STATUS, 0L
                ));
        when(adRepository.countByStatus("active")).thenReturn(120L);
        when(adRepository.countByStatus("deleted")).thenReturn(4L);
        when(adRepository.countEnrichedAds()).thenReturn(50L);
        when(downstreamEventRepository.createPendingEvent(anyString(), any(), any(), anyString(), any(), anyString()))
                .thenReturn(true);

        ReportGenerationService service = new ReportGenerationService(
                downstreamEventRepository,
                jobExecutionStateRepository,
                adRepository,
                catalog(),
                new NotificationFormatter(),
                properties(),
                new OperationalMetricsRecorder(new SimpleMeterRegistry()),
                Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC)
        );

        service.queueDailySummaryForPreviousDay();

        verify(downstreamEventRepository).createPendingEvent(
                org.mockito.ArgumentMatchers.eq("daily-summary:2026-03-14"),
                org.mockito.ArgumentMatchers.eq(DownstreamEventType.DAILY_SUMMARY),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("PROD"),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.parse("2026-03-14T23:59:59")),
                org.mockito.ArgumentMatchers.contains("Daily summary for 2026-03-14")
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
        properties.setEnabled(true);
        properties.setDailySummaryEnabled(true);
        properties.setEnvironmentName("PROD");
        return properties;
    }
}
