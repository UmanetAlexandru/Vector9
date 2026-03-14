package md.hashcode.vector9.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import md.hashcode.vector9.jooq.tables.records.AdsRecord;
import md.hashcode.vector9.model.AdUpsertCommand;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.ADS;

@Repository
public class AdRepository {

    private final DSLContext dslContext;

    public AdRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Optional<AdsRecord> findById(long id) {
        return dslContext.selectFrom(ADS)
                .where(ADS.ID.eq(id))
                .fetchOptionalInto(AdsRecord.class);
    }

    public List<AdsRecord> findActiveAdsLastSeenBefore(LocalDateTime cutoff) {
        return dslContext.selectFrom(ADS)
                .where(ADS.STATUS.eq("active"))
                .and(ADS.LAST_SEEN_AT.isNotNull())
                .and(ADS.LAST_SEEN_AT.lt(cutoff))
                .orderBy(ADS.ID.asc())
                .fetchInto(AdsRecord.class);
    }

    public List<AdsRecord> findActiveAdsForViewTracking() {
        return dslContext.selectFrom(ADS)
                .where(ADS.STATUS.eq("active"))
                .orderBy(ADS.ID.asc())
                .fetchInto(AdsRecord.class);
    }

    public int markDeleted(Collection<Long> adIds, LocalDateTime now) {
        if (adIds == null || adIds.isEmpty()) {
            return 0;
        }

        return dslContext.update(ADS)
                .set(ADS.STATUS, "deleted")
                .set(ADS.UPDATED_AT, now)
                .where(ADS.ID.in(adIds))
                .and(ADS.STATUS.eq("active"))
                .execute();
    }

    public int updateViewCounters(long adId,
                                  Integer viewsToday,
                                  Integer viewsTotal,
                                  Integer viewsSinceRepublish,
                                  LocalDateTime fetchedAt) {
        var update = dslContext.update(ADS)
                .set(ADS.VIEWS_TOTAL, viewsTotal)
                .set(ADS.VIEWS_SINCE_REPUBLISH, viewsSinceRepublish)
                .set(ADS.VIEWS_LAST_FETCHED_AT, fetchedAt)
                .set(ADS.UPDATED_AT, fetchedAt);

        if (viewsToday != null) {
            update.set(ADS.VIEWS_TODAY, viewsToday);
        }

        return update.where(ADS.ID.eq(adId))
                .execute();
    }

    public AdsRecord upsert(AdUpsertCommand command, UUID ownerId, LocalDateTime now) {
        LocalDateTime lastSeenAt = command.lastSeenAt() != null ? command.lastSeenAt() : now;
        String status = command.status() != null ? command.status() : "active";

        return dslContext.insertInto(ADS)
                .set(ADS.ID, command.id())
                .set(ADS.TITLE, command.title())
                .set(ADS.SUBCATEGORY_ID, command.subcategoryId())
                .set(ADS.PRICE_VALUE, command.priceValue())
                .set(ADS.PRICE_UNIT, command.priceUnit())
                .set(ADS.PRICE_MEASUREMENT, command.priceMeasurement())
                .set(ADS.PRICE_MODE, command.priceMode())
                .set(ADS.PRICE_PER_METER, command.pricePerMeter())
                .set(ADS.OLD_PRICE_VALUE, command.oldPriceValue())
                .set(ADS.BODY_RO, command.bodyRo())
                .set(ADS.BODY_RU, command.bodyRu())
                .set(ADS.AD_STATE, command.adState())
                .set(ADS.OFFER_TYPE_ID, command.offerTypeId())
                .set(ADS.OFFER_TYPE_VALUE, command.offerTypeValue())
                .set(ADS.OFFER_TYPE_TEXT, command.offerTypeText())
                .set(ADS.OWNER_ID, ownerId)
                .set(ADS.TRANSPORT_YEAR, command.transportYear())
                .set(ADS.REAL_ESTATE_TYPE, command.realEstateType())
                .set(ADS.STATUS, status)
                .set(ADS.LAST_SEEN_AT, lastSeenAt)
                .set(ADS.LAST_UPDATED_AT, now)
                .set(ADS.FIRST_SEEN_AT, now)
                .set(ADS.UPDATED_AT, now)
                .onConflict(ADS.ID)
                .doUpdate()
                .set(ADS.TITLE, command.title())
                .set(ADS.SUBCATEGORY_ID, command.subcategoryId())
                .set(ADS.PRICE_VALUE, command.priceValue())
                .set(ADS.PRICE_UNIT, command.priceUnit())
                .set(ADS.PRICE_MEASUREMENT, command.priceMeasurement())
                .set(ADS.PRICE_MODE, command.priceMode())
                .set(ADS.PRICE_PER_METER, command.pricePerMeter())
                .set(ADS.OLD_PRICE_VALUE, command.oldPriceValue())
                .set(ADS.BODY_RO, command.bodyRo())
                .set(ADS.BODY_RU, command.bodyRu())
                .set(ADS.AD_STATE, command.adState())
                .set(ADS.OFFER_TYPE_ID, command.offerTypeId())
                .set(ADS.OFFER_TYPE_VALUE, command.offerTypeValue())
                .set(ADS.OFFER_TYPE_TEXT, command.offerTypeText())
                .set(ADS.OWNER_ID, ownerId)
                .set(ADS.TRANSPORT_YEAR, command.transportYear())
                .set(ADS.REAL_ESTATE_TYPE, command.realEstateType())
                .set(ADS.STATUS, status)
                .set(ADS.LAST_SEEN_AT, lastSeenAt)
                .set(ADS.LAST_UPDATED_AT, now)
                .set(ADS.UPDATED_AT, now)
                .returning()
                .fetchOne();
    }
}
