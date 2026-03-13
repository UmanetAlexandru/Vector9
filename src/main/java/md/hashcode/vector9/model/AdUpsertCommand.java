package md.hashcode.vector9.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdUpsertCommand(
        long id,
        String title,
        long subcategoryId,
        BigDecimal priceValue,
        String priceUnit,
        String priceMeasurement,
        String priceMode,
        BigDecimal pricePerMeter,
        BigDecimal oldPriceValue,
        String bodyRo,
        String bodyRu,
        String adState,
        Integer offerTypeId,
        Integer offerTypeValue,
        String offerTypeText,
        Integer transportYear,
        String realEstateType,
        String status,
        LocalDateTime lastSeenAt
) {
}