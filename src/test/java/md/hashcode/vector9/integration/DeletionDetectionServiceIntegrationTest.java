package md.hashcode.vector9.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.service.DeletionDetectionService;
import md.hashcode.vector9.service.TrackingProperties;
import org.junit.jupiter.api.Test;

import static md.hashcode.vector9.jooq.Tables.ADS;
import static org.assertj.core.api.Assertions.assertThat;

class DeletionDetectionServiceIntegrationTest extends RepositoryIntegrationTestSupport {

    @Test
    void shouldMarkOnlyStaleActiveAdsDeleted() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 14, 12, 0);
        adRepository.upsert(adCommand(1001L, now.minusDays(10), "active"), null, now);
        adRepository.upsert(adCommand(1002L, now.minusDays(2), "active"), null, now);
        adRepository.upsert(adCommand(1003L, now.minusDays(10), "deleted"), null, now);

        TrackingProperties properties = new TrackingProperties();
        properties.setDeletionThresholdDays(7);
        DeletionDetectionService service = new DeletionDetectionService(
                adRepository,
                properties,
                Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC)
        );

        var result = service.markStaleAdsDeleted();

        assertThat(result.adsChecked()).isEqualTo(1);
        assertThat(result.adsMarkedDeleted()).isEqualTo(1);
        assertThat(dslContext.select(ADS.STATUS).from(ADS).where(ADS.ID.eq(1001L)).fetchOne(ADS.STATUS)).isEqualTo("deleted");
        assertThat(dslContext.select(ADS.STATUS).from(ADS).where(ADS.ID.eq(1002L)).fetchOne(ADS.STATUS)).isEqualTo("active");
        assertThat(dslContext.select(ADS.STATUS).from(ADS).where(ADS.ID.eq(1003L)).fetchOne(ADS.STATUS)).isEqualTo("deleted");
    }

    private AdUpsertCommand adCommand(long id, LocalDateTime lastSeenAt, String status) {
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
                status,
                lastSeenAt
        );
    }
}
