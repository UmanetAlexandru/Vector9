package md.hashcode.vector9.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.PRICE_HISTORY;

@Repository
public class PriceHistoryRepository {

    private final DSLContext dslContext;

    public PriceHistoryRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public void insertChange(long adId, BigDecimal oldPrice, BigDecimal newPrice, String priceUnit, LocalDateTime changedAt) {
        dslContext.insertInto(PRICE_HISTORY)
                .set(PRICE_HISTORY.AD_ID, adId)
                .set(PRICE_HISTORY.OLD_PRICE, oldPrice)
                .set(PRICE_HISTORY.NEW_PRICE, newPrice)
                .set(PRICE_HISTORY.PRICE_UNIT, priceUnit)
                .set(PRICE_HISTORY.CHANGED_AT, changedAt)
                .execute();
    }
}