package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.GraphqlClientException;
import md.hashcode.vector9.jooq.tables.records.AdsRecord;
import md.hashcode.vector9.model.graphql.AdViewsData;
import md.hashcode.vector9.model.graphql.GraphqlAdViews;
import md.hashcode.vector9.model.graphql.GraphqlResponse;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.ViewHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewTrackingServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private ViewHistoryRepository viewHistoryRepository;
    @Mock
    private GraphqlClient graphqlClient;

    @Test
    void shouldBatchActiveAdsAndPersistViews() {
        TrackingProperties properties = new TrackingProperties();
        properties.setViewBatchSize(2);
        properties.setWriteViewHistory(true);
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);

        when(adRepository.findActiveAdsForViewTracking()).thenReturn(List.of(ad(1L), ad(2L), ad(3L)));
        when(graphqlClient.adViews(any()))
                .thenReturn(new GraphqlResponse<>(new AdViewsData(List.of(
                        new GraphqlAdViews(1L, 1, 10, 3),
                        new GraphqlAdViews(2L, 2, 20, 4)
                )), List.of()))
                .thenReturn(new GraphqlResponse<>(new AdViewsData(List.of(
                        new GraphqlAdViews(3L, null, 30, 5)
                )), List.of()));
        when(adRepository.updateViewCounters(any(Long.class), any(), any(), any(), any())).thenReturn(1);

        ViewTrackingService service = new ViewTrackingService(
                adRepository,
                viewHistoryRepository,
                graphqlClient,
                properties,
                clock
        );

        ViewTrackingJobResult result = service.refreshActiveAdViews();

        assertThat(result.adsRequested()).isEqualTo(3);
        assertThat(result.adsUpdated()).isEqualTo(3);
        assertThat(result.historyRowsInserted()).isEqualTo(3);
        assertThat(result.batchesAttempted()).isEqualTo(2);
        assertThat(result.batchesSucceeded()).isEqualTo(2);
        assertThat(result.batchesFailed()).isZero();
        assertThat(result.failureMessages()).isEmpty();
        verify(graphqlClient, times(2)).adViews(any());
        verify(viewHistoryRepository, times(3)).insertSnapshot(any(Long.class), any(Integer.class), any(Integer.class), any());
    }

    @Test
    void shouldContinueAfterFailedBatch() {
        TrackingProperties properties = new TrackingProperties();
        properties.setViewBatchSize(2);
        properties.setWriteViewHistory(false);
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);

        when(adRepository.findActiveAdsForViewTracking()).thenReturn(List.of(ad(1L), ad(2L), ad(3L)));
        when(graphqlClient.adViews(any()))
                .thenThrow(new GraphqlClientException("http 500"))
                .thenReturn(new GraphqlResponse<>(new AdViewsData(List.of(
                        new GraphqlAdViews(3L, 4, 30, 7)
                )), List.of()));
        when(adRepository.updateViewCounters(any(Long.class), any(), any(), any(), any())).thenReturn(1);

        ViewTrackingService service = new ViewTrackingService(
                adRepository,
                viewHistoryRepository,
                graphqlClient,
                properties,
                clock
        );

        ViewTrackingJobResult result = service.refreshActiveAdViews();

        assertThat(result.adsRequested()).isEqualTo(3);
        assertThat(result.adsUpdated()).isEqualTo(1);
        assertThat(result.historyRowsInserted()).isZero();
        assertThat(result.batchesAttempted()).isEqualTo(2);
        assertThat(result.batchesSucceeded()).isEqualTo(1);
        assertThat(result.batchesFailed()).isEqualTo(1);
        assertThat(result.failureMessages()).hasSize(1);
        assertThat(result.failureMessages().getFirst()).contains("http 500");
        verify(viewHistoryRepository, never()).insertSnapshot(any(Long.class), any(Integer.class), any(Integer.class), any());
    }

    private AdsRecord ad(long id) {
        AdsRecord record = new AdsRecord();
        record.setId(id);
        record.setStatus("active");
        return record;
    }
}

