package md.hashcode.vector9.crawler;

import java.util.Objects;

import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import org.springframework.stereotype.Component;

@Component
public class SearchAdsRequestFactory {

    private final CrawlerProperties properties;

    public SearchAdsRequestFactory(CrawlerProperties properties) {
        this.properties = properties;
    }

    public SearchAdsRequest build(SubcategoriesRecord subcategory, int skip) {
        return build(subcategory, skip, properties.getPageSize());
    }

    public SearchAdsRequest build(SubcategoriesRecord subcategory, int skip, int limit) {
        Objects.requireNonNull(subcategory, "subcategory");

        return SearchAdsRequest.builder()
                .subcategoryId(subcategory.getId())
                .limit(limit)
                .skip(skip)
                .locale(properties.getLocale())
                .includeOwner(true)
                .includeBody(false)
                .includeBoost(false)
                .includeCarsFeatures(Boolean.TRUE.equals(subcategory.getIncludeCarsFeatures()))
                .inputSource(properties.getInputSource())
                .headerLanguage(properties.getLanguage())
                .headerSource(properties.getSourceHeader());
    }
}