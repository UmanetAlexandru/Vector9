package md.hashcode.vector9.crawler;

import java.util.List;

import md.hashcode.vector9.client.GraphqlAdMapper;
import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.ProcessedAdResult;
import md.hashcode.vector9.model.graphql.GraphqlAd;
import md.hashcode.vector9.model.graphql.GraphqlResponse;
import md.hashcode.vector9.model.graphql.SearchAdsData;
import md.hashcode.vector9.model.graphql.SearchAdsResult;
import md.hashcode.vector9.repository.SubcategoryRepository;
import md.hashcode.vector9.service.AdPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalCrawlerTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;
    @Mock
    private GraphqlClient graphqlClient;
    @Mock
    private GraphqlAdMapper graphqlAdMapper;
    @Mock
    private AdPersistenceService adPersistenceService;

    private IncrementalCrawler crawler;
    private SubcategoriesRecord subcategory;

    @BeforeEach
    void setUp() {
        CrawlerProperties properties = new CrawlerProperties();
        properties.setPageSize(10);
        properties.setUnchangedStopThreshold(3);
        crawler = new IncrementalCrawler(
                subcategoryRepository,
                graphqlClient,
                graphqlAdMapper,
                adPersistenceService,
                new SearchAdsRequestFactory(properties),
                properties
        );

        subcategory = new SubcategoriesRecord();
        subcategory.setId(7661L);
        subcategory.setEnabled(true);
        subcategory.setIncludeCarsFeatures(false);
    }

    @Test
    void shouldStopEarlyAfterConfiguredConsecutiveUnchangedAds() {
        when(graphqlClient.searchAds(any(SearchAdsRequest.class))).thenReturn(response(page(5, 100)));
        when(graphqlAdMapper.toProcessedAds(any())).thenReturn(List.of(
                processedAd(1L),
                processedAd(2L),
                processedAd(3L),
                processedAd(4L),
                processedAd(5L)
        ));
        when(adPersistenceService.persist(any())).thenReturn(
                new ProcessedAdResult(false, false, false, 0),
                new ProcessedAdResult(false, false, false, 0),
                new ProcessedAdResult(false, false, false, 0)
        );

        SubcategoryCrawlResult result = crawler.crawlSubcategory(subcategory);

        assertThat(result.completed()).isFalse();
        assertThat(result.stoppedEarly()).isTrue();
        assertThat(result.adsFetched()).isEqualTo(5);
        assertThat(result.adsProcessed()).isEqualTo(3);
        assertThat(result.unchangedAds()).isEqualTo(3);
    }

    @Test
    void shouldResetUnchangedCounterWhenNewOrUpdatedAdsAppear() {
        when(graphqlClient.searchAds(any(SearchAdsRequest.class)))
                .thenReturn(response(page(4, 100)))
                .thenReturn(response(page(0, 100)));
        when(graphqlAdMapper.toProcessedAds(any()))
                .thenReturn(List.of(processedAd(1L), processedAd(2L), processedAd(3L), processedAd(4L)))
                .thenReturn(List.of());
        when(adPersistenceService.persist(any())).thenReturn(
                new ProcessedAdResult(false, false, false, 0),
                new ProcessedAdResult(true, true, false, 0),
                new ProcessedAdResult(false, false, false, 0),
                new ProcessedAdResult(false, false, false, 0)
        );

        SubcategoryCrawlResult result = crawler.crawlSubcategory(subcategory);

        assertThat(result.completed()).isTrue();
        assertThat(result.stoppedEarly()).isFalse();
        assertThat(result.newAds()).isEqualTo(1);
        assertThat(result.unchangedAds()).isEqualTo(3);
        assertThat(result.failureMessage()).isNull();
    }

    @Test
    void shouldReturnFailureWithSubcategoryContextOnGraphqlException() {
        when(graphqlClient.searchAds(any(SearchAdsRequest.class))).thenThrow(new IllegalStateException("http 500"));

        SubcategoryCrawlResult result = crawler.crawlSubcategory(subcategory);

        assertThat(result.completed()).isFalse();
        assertThat(result.failureMessage()).contains("7661").contains("http 500");
        verify(subcategoryRepository, never()).findEnabled();
    }

    private GraphqlResponse<SearchAdsData> response(SearchAdsResult result) {
        return new GraphqlResponse<>(new SearchAdsData(result), List.of());
    }

    private SearchAdsResult page(int adsCount, Integer totalCount) {
        return new SearchAdsResult(
                java.util.stream.IntStream.range(0, adsCount)
                        .mapToObj(index -> new GraphqlAd(
                                String.valueOf(index + 1), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ))
                        .toList(),
                totalCount,
                null
        );
    }

    private ProcessedAdCommand processedAd(long id) {
        return new ProcessedAdCommand(
                new AdUpsertCommand(id, "title", 7661L, null, null, null, null, null, null, null, null, "active", null, null, null, null, null, "active", null),
                null,
                List.of()
        );
    }
}
