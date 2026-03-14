package md.hashcode.vector9.repository;

import java.time.LocalDateTime;

import md.hashcode.vector9.jooq.tables.records.ViewHistoryRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.VIEW_HISTORY;

@Repository
public class ViewHistoryRepository {

    private final DSLContext dslContext;

    public ViewHistoryRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public ViewHistoryRecord insertSnapshot(long adId, int viewsTotal, int viewsSinceRepublish, LocalDateTime recordedAt) {
        return dslContext.insertInto(VIEW_HISTORY)
                .set(VIEW_HISTORY.AD_ID, adId)
                .set(VIEW_HISTORY.VIEWS_TOTAL, viewsTotal)
                .set(VIEW_HISTORY.VIEWS_SINCE_REPUBLISH, viewsSinceRepublish)
                .set(VIEW_HISTORY.RECORDED_AT, recordedAt)
                .returning()
                .fetchOne();
    }
}
