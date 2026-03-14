package md.hashcode.vector9.crawler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import md.hashcode.vector9.client.GraphqlAdMapper;
import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.CrawlCheckpointsRecord;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.model.CheckpointUpdateCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.ProcessedAdResult;
import md.hashcode.vector9.model.graphql.GraphqlAd;
import md.hashcode.vector9.model.graphql.GraphqlResponse;
import md.hashcode.vector9.model.graphql.SearchAdsData;
import md.hashcode.vector9.model.graphql.SearchAdsResult;
import md.hashcode.vector9.repository.CrawlCheckpointRepository;
import md.hashcode.vector9.repository.SubcategoryRepository;
import md.hashcode.vector9.service.AdPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialDiscoveryCrawlerTest {

    @Mock
    private SubcategoryRepository subcategoryRepository;
    @Mock
    private CrawlCheckpointRepository crawlCheckpointRepository;
    @Mock
    private GraphqlClient graphqlClient;
    @Mock
    private GraphqlAdMapper graphqlAdMapper;
    @Mock
    private AdPersistenceService adPersistenceService;

    private InitialDiscoveryCrawler crawler;
    private SubcategoriesRecord subcategory;

    @BeforeEach
    void setUp() {
        CrawlerProperties properties = new CrawlerProperties();
        properties.setPageSize(2);
        crawler = new InitialDiscoveryCrawler(
                subcategoryRepository,
                crawlCheckpointRepository,
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
    void shouldStartFromCheckpointSkipAndClearCheckpointOnSuccess() {
        CrawlCheckpointsRecord checkpoint = new CrawlCheckpointsRecord();
        checkpoint.setSubcategoryId(7661L);
        checkpoint.setCurrentSkip(4);
        checkpoint.setAdsProcessed(4);
        when(crawlCheckpointRepository.findBySubcategoryId(7661L)).thenReturn(Optional.of(checkpoint));
        when(graphqlClient.searchAds(any(SearchAdsRequest.class)))
                .thenReturn(response(page(2, 10)))
                .thenReturn(response(page(0, 10)));
        when(graphqlAdMapper.toProcessedAds(any()))
                .thenReturn(List.of(processedAd(1001L), processedAd(1002L)))
                .thenReturn(List.of());
        when(adPersistenceService.persist(any())).thenReturn(
                new ProcessedAdResult(true, true, false, 0),
                new ProcessedAdResult(false, true, true, 0)
        );

        SubcategoryCrawlResult result = crawler.crawlSubcategory(subcategory);

        assertThat(result.completed()).isTrue();
        assertThat(result.stoppedEarly()).isFalse();
        assertThat(result.pagesFetched()).isEqualTo(1);
        assertThat(result.adsFetched()).isEqualTo(2);
        assertThat(result.adsProcessed()).isEqualTo(2);
        assertThat(result.newAds()).isEqualTo(1);
        assertThat(result.updatedAds()).isEqualTo(1);
        assertThat(result.failureMessage()).isNull();

        ArgumentCaptor<SearchAdsRequest> requestCaptor = ArgumentCaptor.forClass(SearchAdsRequest.class);
        verify(graphqlClient, times(2)).searchAds(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().getFirst().toGraphqlRequest().variables().input().pagination().skip()).isEqualTo(4);
        ArgumentCaptor<CheckpointUpdateCommand> checkpointCaptor = ArgumentCaptor.forClass(CheckpointUpdateCommand.class);
        verify(crawlCheckpointRepository).upsert(checkpointCaptor.capture(), any(LocalDateTime.class));
        assertThat(checkpointCaptor.getValue().currentSkip()).isEqualTo(6);
        assertThat(checkpointCaptor.getValue().adsProcessed()).isEqualTo(6);
        verify(crawlCheckpointRepository).clear(7661L);
    }

    @Test
    void shouldPreserveCheckpointOnFailure() {
        when(crawlCheckpointRepository.findBySubcategoryId(7661L)).thenReturn(Optional.empty());
        when(graphqlClient.searchAds(any(SearchAdsRequest.class)))
                .thenReturn(response(page(2, 10)))
                .thenThrow(new IllegalStateException("transport timeout"));
        when(graphqlAdMapper.toProcessedAds(any())).thenReturn(List.of(processedAd(1001L), processedAd(1002L)));
        when(adPersistenceService.persist(any())).thenReturn(new ProcessedAdResult(true, true, false, 0));

        SubcategoryCrawlResult result = crawler.crawlSubcategory(subcategory);

        assertThat(result.completed()).isFalse();
        assertThat(result.failureMessage()).contains("7661").contains("transport timeout");
        verify(crawlCheckpointRepository).upsert(any(CheckpointUpdateCommand.class), any(LocalDateTime.class));
        verify(crawlCheckpointRepository, never()).clear(7661L);
    }

    @Test
    void shouldIsolateFailuresAcrossEnabledSubcategories() {
        SubcategoriesRecord second = new SubcategoriesRecord();
        second.setId(9001L);
        second.setEnabled(true);
        when(subcategoryRepository.findEnabled()).thenReturn(List.of(subcategory, second));
        when(crawlCheckpointRepository.findBySubcategoryId(any(Long.class))).thenReturn(Optional.empty());
        when(graphqlClient.searchAds(any(SearchAdsRequest.class)))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(response(page(0, 0)));
        when(graphqlAdMapper.toProcessedAds(any())).thenReturn(List.of());

        CrawlBatchResult batchResult = crawler.crawlEnabledSubcategories();

        assertThat(batchResult.subcategoriesAttempted()).isEqualTo(2);
        assertThat(batchResult.successes()).isEqualTo(1);
        assertThat(batchResult.failures()).isEqualTo(1);
        assertThat(batchResult.results()).extracting(SubcategoryCrawlResult::subcategoryId).containsExactly(7661L, 9001L);
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
