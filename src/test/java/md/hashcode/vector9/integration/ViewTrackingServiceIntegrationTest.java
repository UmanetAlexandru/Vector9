package md.hashcode.vector9.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.graphql.AdViewsData;
import md.hashcode.vector9.model.graphql.GraphqlAdViews;
import md.hashcode.vector9.model.graphql.GraphqlResponse;
import md.hashcode.vector9.service.TrackingProperties;
import md.hashcode.vector9.service.ViewTrackingService;
import org.junit.jupiter.api.Test;

import static md.hashcode.vector9.jooq.Tables.ADS;
import static md.hashcode.vector9.jooq.Tables.VIEW_HISTORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewTrackingServiceIntegrationTest extends RepositoryIntegrationTestSupport {

    @Test
    void shouldUpdateCurrentCountersAndInsertHistory() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 14, 12, 0);
        adRepository.upsert(adCommand(1001L, now), null, now);
        adRepository.upsert(adCommand(1002L, now), null, now);

        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(graphqlClient.adViews(any())).thenReturn(new GraphqlResponse<>(new AdViewsData(List.of(
                new GraphqlAdViews(1001L, 3, 111, 11),
                new GraphqlAdViews(1002L, null, 222, 22)
        )), List.of()));

        TrackingProperties properties = new TrackingProperties();
        properties.setViewBatchSize(10);
        properties.setWriteViewHistory(true);

        ViewTrackingService service = new ViewTrackingService(
                adRepository,
                viewHistoryRepository,
                graphqlClient,
                properties,
                Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC)
        );

        var result = service.refreshActiveAdViews();

        assertThat(result.adsRequested()).isEqualTo(2);
        assertThat(result.adsUpdated()).isEqualTo(2);
        assertThat(result.historyRowsInserted()).isEqualTo(2);
        assertThat(dslContext.select(ADS.VIEWS_TOTAL).from(ADS).where(ADS.ID.eq(1001L)).fetchOne(ADS.VIEWS_TOTAL)).isEqualTo(111);
        assertThat(dslContext.select(ADS.VIEWS_SINCE_REPUBLISH).from(ADS).where(ADS.ID.eq(1002L)).fetchOne(ADS.VIEWS_SINCE_REPUBLISH)).isEqualTo(22);
        assertThat(dslContext.select(ADS.VIEWS_TODAY).from(ADS).where(ADS.ID.eq(1001L)).fetchOne(ADS.VIEWS_TODAY)).isEqualTo(3);
        assertThat(dslContext.fetchCount(VIEW_HISTORY)).isEqualTo(2);
    }

    private AdUpsertCommand adCommand(long id, LocalDateTime lastSeenAt) {
        return new AdUpsertCommand(
                id,
                "tracked-ad",
                7661L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                null,
                null,
                null,
                "active",
                lastSeenAt
        );
    }
}
