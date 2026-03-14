package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import md.hashcode.vector9.jooq.tables.records.AdsRecord;
import md.hashcode.vector9.repository.AdRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletionDetectionServiceTest {

    @Mock
    private AdRepository adRepository;

    @Test
    void shouldMarkOnlyStaleAdsAsDeleted() {
        TrackingProperties properties = new TrackingProperties();
        properties.setDeletionThresholdDays(7);
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);

        AdsRecord staleOne = new AdsRecord();
        staleOne.setId(101L);
        AdsRecord staleTwo = new AdsRecord();
        staleTwo.setId(202L);

        when(adRepository.findActiveAdsLastSeenBefore(LocalDateTime.of(2026, 3, 7, 12, 0))).thenReturn(List.of(staleOne, staleTwo));
        when(adRepository.markDeleted(List.of(101L, 202L), LocalDateTime.of(2026, 3, 14, 12, 0))).thenReturn(2);

        DeletionDetectionService service = new DeletionDetectionService(adRepository, properties, clock);

        DeletionJobResult result = service.markStaleAdsDeleted();

        assertThat(result.adsChecked()).isEqualTo(2);
        assertThat(result.adsMarkedDeleted()).isEqualTo(2);
        assertThat(result.thresholdDays()).isEqualTo(7);
        assertThat(result.failureMessage()).isNull();
    }

    @Test
    void shouldReturnFailureMessageWhenRepositoryFails() {
        TrackingProperties properties = new TrackingProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);
        when(adRepository.findActiveAdsLastSeenBefore(any())).thenThrow(new IllegalStateException("db unavailable"));

        DeletionDetectionService service = new DeletionDetectionService(adRepository, properties, clock);

        DeletionJobResult result = service.markStaleAdsDeleted();

        assertThat(result.adsChecked()).isZero();
        assertThat(result.adsMarkedDeleted()).isZero();
        assertThat(result.failureMessage()).contains("db unavailable");
        verify(adRepository).findActiveAdsLastSeenBefore(any());
    }
}
