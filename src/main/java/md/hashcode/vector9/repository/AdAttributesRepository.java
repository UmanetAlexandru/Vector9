package md.hashcode.vector9.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import md.hashcode.vector9.jooq.tables.records.AdAttributesRecord;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import static md.hashcode.vector9.jooq.Tables.AD_ATTRIBUTES;

@Repository
public class AdAttributesRepository {

    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public AdAttributesRepository(DSLContext dslContext, ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    public Optional<AdAttributesRecord> findByAdId(long adId) {
        return dslContext.selectFrom(AD_ATTRIBUTES)
                .where(AD_ATTRIBUTES.AD_ID.eq(adId))
                .fetchOptionalInto(AdAttributesRecord.class);
    }

    public void upsert(long adId,
                       Map<String, Object> characteristics,
                       Map<String, Object> location,
                       Map<String, Object> contactInfo,
                       LocalDateTime enrichedAt,
                       Integer scrapeDurationMs) {
        try {
            JSONB characteristicsJson = JSONB.valueOf(objectMapper.writeValueAsString(characteristics));
            JSONB locationJson = location == null ? null : JSONB.valueOf(objectMapper.writeValueAsString(location));
            JSONB contactInfoJson = contactInfo == null ? null : JSONB.valueOf(objectMapper.writeValueAsString(contactInfo));

            dslContext.insertInto(AD_ATTRIBUTES)
                    .set(AD_ATTRIBUTES.AD_ID, adId)
                    .set(AD_ATTRIBUTES.CHARACTERISTICS, characteristicsJson)
                    .set(AD_ATTRIBUTES.LOCATION, locationJson)
                    .set(AD_ATTRIBUTES.CONTACT_INFO, contactInfoJson)
                    .set(AD_ATTRIBUTES.ENRICHED_AT, enrichedAt)
                    .set(AD_ATTRIBUTES.SCRAPE_DURATION_MS, scrapeDurationMs)
                    .set(AD_ATTRIBUTES.UPDATED_AT, enrichedAt)
                    .onConflict(AD_ATTRIBUTES.AD_ID)
                    .doUpdate()
                    .set(AD_ATTRIBUTES.CHARACTERISTICS, characteristicsJson)
                    .set(AD_ATTRIBUTES.LOCATION, locationJson)
                    .set(AD_ATTRIBUTES.CONTACT_INFO, contactInfoJson)
                    .set(AD_ATTRIBUTES.ENRICHED_AT, enrichedAt)
                    .set(AD_ATTRIBUTES.SCRAPE_DURATION_MS, scrapeDurationMs)
                    .set(AD_ATTRIBUTES.UPDATED_AT, enrichedAt)
                    .execute();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist ad attributes for ad %s".formatted(adId), exception);
        }
    }
}
