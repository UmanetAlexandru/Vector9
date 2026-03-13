package md.hashcode.vector9.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.graphql.GraphqlAd;
import md.hashcode.vector9.model.graphql.GraphqlBusiness;
import md.hashcode.vector9.model.graphql.GraphqlCategory;
import md.hashcode.vector9.model.graphql.GraphqlFeatureValue;
import md.hashcode.vector9.model.graphql.GraphqlOwner;
import md.hashcode.vector9.model.graphql.GraphqlTranslation;
import md.hashcode.vector9.model.graphql.GraphqlVerification;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GraphqlAdMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GraphqlAdMapper mapper = new GraphqlAdMapper();

    @Test
    void shouldMapObservedPriceOwnerAndImages() throws Exception {
        GraphqlAd ad = new GraphqlAd(
                "103465316",
                "Bmax B1 Pro 8/128Gb - doar 1699 lei!",
                new GraphqlCategory(7661L, new GraphqlTranslation("Mini PC"), null),
                new GraphqlFeatureValue(2L, "FEATURE_PRICE", objectMapper.readTree("""
                        {"bargain":false,"down_payment":0,"measurement":"UNIT_MDL","mode":"PM_FIXED","unit":"UNIT_MDL","value":1699}
                        """)),
                null,
                new GraphqlFeatureValue(1640L, "FEATURE_INT", objectMapper.readTree("1999")),
                new GraphqlFeatureValue(14L, "FEATURE_IMAGES", objectMapper.readTree("""
                        ["a.jpg?metadata=123","b.jpg"]
                        """)),
                new GraphqlOwner(
                        UUID.randomUUID().toString(),
                        "seller-mini",
                        "avatar.png",
                        "2026-01-01",
                        new GraphqlBusiness("PREMIUM", "biz-1"),
                        new GraphqlVerification(true, "1711111111")
                ),
                null,
                new GraphqlFeatureValue(795L, "FEATURE_OPTIONS", objectMapper.readTree("""
                        {"translated":"Magazin","value":37797}
                        """)),
                null,
                null,
                null,
                null,
                null,
                "13 mar. 2026, 20:37"
        );

        ProcessedAdCommand mapped = mapper.toProcessedAd(ad);

        assertThat(mapped.ad().id()).isEqualTo(103465316L);
        assertThat(mapped.ad().priceValue()).isEqualByComparingTo(new BigDecimal("1699"));
        assertThat(mapped.ad().priceUnit()).isEqualTo("UNIT_MDL");
        assertThat(mapped.ad().priceMeasurement()).isEqualTo("UNIT_MDL");
        assertThat(mapped.ad().priceMode()).isEqualTo("PM_FIXED");
        assertThat(mapped.ad().oldPriceValue()).isEqualByComparingTo(new BigDecimal("1999"));
        assertThat(mapped.ad().realEstateType()).isEqualTo("Magazin");
        assertThat(mapped.images()).extracting(image -> image.filename()).containsExactly("a.jpg", "b.jpg");
        assertThat(mapped.owner()).isNotNull();
        assertThat(mapped.owner().businessPlan()).isEqualTo("PREMIUM");
        assertThat(mapped.owner().verified()).isTrue();
    }

    @Test
    void shouldReturnEmptyForNoAds() {
        assertThat(mapper.toProcessedAds(List.of())).isEmpty();
        assertThat(mapper.toProcessedAds(null)).isEmpty();
    }
}
