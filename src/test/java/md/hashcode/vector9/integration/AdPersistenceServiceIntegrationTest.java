package md.hashcode.vector9.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import md.hashcode.vector9.model.AdImageInput;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.OwnerUpsertCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import org.junit.jupiter.api.Test;

import static md.hashcode.vector9.jooq.Tables.AD_IMAGES;
import static md.hashcode.vector9.jooq.Tables.ADS;
import static md.hashcode.vector9.jooq.Tables.OWNERS;
import static md.hashcode.vector9.jooq.Tables.PRICE_HISTORY;
import static org.assertj.core.api.Assertions.assertThat;

class AdPersistenceServiceIntegrationTest extends RepositoryIntegrationTestSupport {

    @Test
    void shouldPersistOwnerAdAndImages() {
        UUID ownerId = UUID.randomUUID();

        var result = adPersistenceService.persist(new ProcessedAdCommand(
                adCommand(1001L, "Mini PC M4", new BigDecimal("450.00"), "EUR"),
                ownerCommand(ownerId, "seller-mini"),
                List.of(
                        new AdImageInput("front.jpg", 0, true),
                        new AdImageInput("side.jpg", 1, false)
                )
        ));

        assertThat(result.created()).isTrue();
        assertThat(result.priceChanged()).isFalse();
        assertThat(result.insertedImages()).isEqualTo(2);
        assertThat(dslContext.fetchCount(OWNERS)).isEqualTo(1);
        assertThat(dslContext.fetchCount(ADS)).isEqualTo(1);
        assertThat(dslContext.fetchCount(AD_IMAGES)).isEqualTo(2);
    }

    @Test
    void shouldBeIdempotentForUnchangedAdAndImages() {
        UUID ownerId = UUID.randomUUID();
        ProcessedAdCommand command = new ProcessedAdCommand(
                adCommand(1002L, "Mini PC M5", new BigDecimal("550.00"), "EUR"),
                ownerCommand(ownerId, "seller-repeat"),
                List.of(
                        new AdImageInput("main.jpg", 0, true),
                        new AdImageInput("rear.jpg", 1, false)
                )
        );

        adPersistenceService.persist(command);
        var secondResult = adPersistenceService.persist(command);

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.priceChanged()).isFalse();
        assertThat(secondResult.insertedImages()).isZero();
        assertThat(dslContext.fetchCount(ADS)).isEqualTo(1);
        assertThat(dslContext.fetchCount(AD_IMAGES)).isEqualTo(2);
        assertThat(dslContext.fetchCount(PRICE_HISTORY)).isZero();
    }

    @Test
    void shouldWritePriceHistoryOnceWhenPriceChangesAndAppendOnlyImages() {
        UUID ownerId = UUID.randomUUID();
        adPersistenceService.persist(new ProcessedAdCommand(
                adCommand(1003L, "Mini PC Pro", new BigDecimal("650.00"), "EUR"),
                ownerCommand(ownerId, "seller-price"),
                List.of(
                        new AdImageInput("a.jpg", 0, true),
                        new AdImageInput("b.jpg", 1, false)
                )
        ));

        var changedResult = adPersistenceService.persist(new ProcessedAdCommand(
                adCommand(1003L, "Mini PC Pro", new BigDecimal("625.00"), "EUR"),
                ownerCommand(ownerId, "seller-price"),
                List.of(
                        new AdImageInput("a.jpg", 0, true),
                        new AdImageInput("b.jpg", 1, false),
                        new AdImageInput("c.jpg", 2, false)
                )
        ));
        var repeatedChangedResult = adPersistenceService.persist(new ProcessedAdCommand(
                adCommand(1003L, "Mini PC Pro", new BigDecimal("625.00"), "EUR"),
                ownerCommand(ownerId, "seller-price"),
                List.of(
                        new AdImageInput("a.jpg", 0, true),
                        new AdImageInput("b.jpg", 1, false),
                        new AdImageInput("c.jpg", 2, false)
                )
        ));

        assertThat(changedResult.priceChanged()).isTrue();
        assertThat(changedResult.insertedImages()).isEqualTo(1);
        assertThat(repeatedChangedResult.priceChanged()).isFalse();
        assertThat(repeatedChangedResult.insertedImages()).isZero();
        assertThat(dslContext.fetchCount(PRICE_HISTORY)).isEqualTo(1);
        assertThat(dslContext.fetchCount(AD_IMAGES)).isEqualTo(3);
        assertThat(dslContext.select(PRICE_HISTORY.OLD_PRICE, PRICE_HISTORY.NEW_PRICE)
                .from(PRICE_HISTORY)
                .fetchOne())
                .extracting(record -> record.get(PRICE_HISTORY.OLD_PRICE), record -> record.get(PRICE_HISTORY.NEW_PRICE))
                .containsExactly(new BigDecimal("650.00"), new BigDecimal("625.00"));
    }

    private OwnerUpsertCommand ownerCommand(UUID ownerId, String login) {
        return new OwnerUpsertCommand(ownerId, login, "avatar.png", "2024-01-01", "basic", "biz-1", true, "2026-03-13");
    }

    private AdUpsertCommand adCommand(long adId, String title, BigDecimal priceValue, String priceUnit) {
        return new AdUpsertCommand(
                adId,
                title,
                7661L,
                priceValue,
                priceUnit,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                1,
                1,
                "sale",
                null,
                null,
                "active",
                LocalDateTime.of(2026, 3, 13, 8, 0)
        );
    }
}