package md.hashcode.vector9.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchAdsRequestTest {

    @Test
    void shouldUseExpectedDefaults() {
        GraphqlRequest<SearchAdsRequest.SearchAdsVariables> request = SearchAdsRequest.builder()
                .subcategoryId(7661L)
                .toGraphqlRequest();

        assertThat(request.operationName()).isEqualTo("SearchAds");
        assertThat(request.language()).isEqualTo("ro");
        assertThat(request.sourceHeader()).isEqualTo("desktop");
        assertThat(request.variables().includeOwner()).isTrue();
        assertThat(request.variables().includeBody()).isFalse();
        assertThat(request.variables().includeCarsFeatures()).isFalse();
        assertThat(request.variables().includeBoost()).isFalse();
        assertThat(request.variables().locale()).isEqualTo("ro_RO");
        assertThat(request.variables().input().source()).isEqualTo("AD_SOURCE_DESKTOP");
    }

    @Test
    void shouldSerializeObservedFilterShape() {
        GraphqlRequest<SearchAdsRequest.SearchAdsVariables> request = SearchAdsRequest.builder()
                .subcategoryId(7661L)
                .limit(78)
                .skip(0)
                .addFilter(16L, 1L, List.of(776L))
                .toGraphqlRequest();

        assertThat(request.variables().input().filters()).hasSize(1);
        SearchAdsRequest.FilterGroup group = request.variables().input().filters().getFirst();
        assertThat(group.filterId()).isEqualTo(16L);
        assertThat(group.features()).hasSize(1);
        assertThat(group.features().getFirst().featureId()).isEqualTo(1L);
        assertThat(group.features().getFirst().optionIds()).containsExactly(776L);
        assertThat(request.variables().input().pagination().limit()).isEqualTo(78);
        assertThat(request.variables().input().pagination().skip()).isZero();
    }
}
