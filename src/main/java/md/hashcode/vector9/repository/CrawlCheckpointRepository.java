package md.hashcode.vector9.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import md.hashcode.vector9.jooq.tables.records.CrawlCheckpointsRecord;
import md.hashcode.vector9.model.CheckpointUpdateCommand;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.CRAWL_CHECKPOINTS;

@Repository
public class CrawlCheckpointRepository {

    private final DSLContext dslContext;

    public CrawlCheckpointRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Optional<CrawlCheckpointsRecord> findBySubcategoryId(long subcategoryId) {
        return dslContext.selectFrom(CRAWL_CHECKPOINTS)
                .where(CRAWL_CHECKPOINTS.SUBCATEGORY_ID.eq(subcategoryId))
                .fetchOptionalInto(CrawlCheckpointsRecord.class);
    }

    public CrawlCheckpointsRecord upsert(CheckpointUpdateCommand command, LocalDateTime now) {
        return dslContext.insertInto(CRAWL_CHECKPOINTS)
                .set(CRAWL_CHECKPOINTS.SUBCATEGORY_ID, command.subcategoryId())
                .set(CRAWL_CHECKPOINTS.CURRENT_SKIP, command.currentSkip())
                .set(CRAWL_CHECKPOINTS.TOTAL_ADS_COUNT, command.totalAdsCount())
                .set(CRAWL_CHECKPOINTS.ADS_PROCESSED, command.adsProcessed())
                .set(CRAWL_CHECKPOINTS.LAST_CHECKPOINT_AT, now)
                .onConflict(CRAWL_CHECKPOINTS.SUBCATEGORY_ID)
                .doUpdate()
                .set(CRAWL_CHECKPOINTS.CURRENT_SKIP, command.currentSkip())
                .set(CRAWL_CHECKPOINTS.TOTAL_ADS_COUNT, command.totalAdsCount())
                .set(CRAWL_CHECKPOINTS.ADS_PROCESSED, command.adsProcessed())
                .set(CRAWL_CHECKPOINTS.LAST_CHECKPOINT_AT, now)
                .returning()
                .fetchOne();
    }

    public void clear(long subcategoryId) {
        dslContext.deleteFrom(CRAWL_CHECKPOINTS)
                .where(CRAWL_CHECKPOINTS.SUBCATEGORY_ID.eq(subcategoryId))
                .execute();
    }
}