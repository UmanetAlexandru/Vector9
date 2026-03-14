package md.hashcode.vector9.crawler;

import md.hashcode.vector9.client.GraphqlRequest;
import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchAdsRequestFactoryTest {

    @Test
    void shouldBuildCategoryAwareRequest() {
        CrawlerProperties properties = new CrawlerProperties();
        properties.setPageSize(78);
        properties.setLocale("ro_RO");
        properties.setLanguage("ro");
        properties.setSourceHeader("desktop");
        properties.setInputSource("AD_SOURCE_DESKTOP");

        SearchAdsRequestFactory factory = new SearchAdsRequestFactory(properties);
        SubcategoriesRecord subcategory = new SubcategoriesRecord();
        subcategory.setId(90210L);
        subcategory.setIncludeCarsFeatures(true);

        GraphqlRequest<SearchAdsRequest.SearchAdsVariables> request = factory.build(subcategory, 156).toGraphqlRequest();
        SearchAdsRequest.SearchAdsVariables variables = request.variables();

        assertThat(request.language()).isEqualTo("ro");
        assertThat(request.sourceHeader()).isEqualTo("desktop");
        assertThat(variables.locale()).isEqualTo("ro_RO");
        assertThat(variables.includeOwner()).isTrue();
        assertThat(variables.includeBody()).isFalse();
        assertThat(variables.includeBoost()).isFalse();
        assertThat(variables.includeCarsFeatures()).isTrue();
        assertThat(variables.input().source()).isEqualTo("AD_SOURCE_DESKTOP");
        assertThat(variables.input().pagination().limit()).isEqualTo(78);
        assertThat(variables.input().pagination().skip()).isEqualTo(156);
        assertThat(variables.input().subCategoryId()).isEqualTo(90210L);
    }
}
