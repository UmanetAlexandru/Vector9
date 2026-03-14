package md.hashcode.vector9.service;

import java.util.List;

import md.hashcode.vector9.crawler.CrawlBatchResult;
import md.hashcode.vector9.crawler.IncrementalCrawler;
import md.hashcode.vector9.crawler.InitialDiscoveryCrawler;
import md.hashcode.vector9.crawler.SubcategoryCrawlResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlOrchestratorTest {

    @Mock
    private InitialDiscoveryCrawler initialDiscoveryCrawler;
    @Mock
    private IncrementalCrawler incrementalCrawler;

    @Test
    void shouldDelegateInitialDiscoveryRuns() {
        CrawlBatchResult expected = new CrawlBatchResult(
                1,
                1,
                0,
                List.of(new SubcategoryCrawlResult(7661L, 2, 2, 1, 1, 0, 1, true, false, null))
        );
        when(initialDiscoveryCrawler.crawlEnabledSubcategories()).thenReturn(expected);

        CrawlOrchestrator orchestrator = new CrawlOrchestrator(initialDiscoveryCrawler, incrementalCrawler);

        assertThat(orchestrator.runInitialDiscovery()).isEqualTo(expected);
        verify(initialDiscoveryCrawler).crawlEnabledSubcategories();
    }

    @Test
    void shouldDelegateIncrementalRefreshRuns() {
        CrawlBatchResult expected = new CrawlBatchResult(
                1,
                1,
                0,
                List.of(new SubcategoryCrawlResult(7661L, 5, 3, 0, 0, 3, 1, false, true, null))
        );
        when(incrementalCrawler.crawlEnabledSubcategories()).thenReturn(expected);

        CrawlOrchestrator orchestrator = new CrawlOrchestrator(initialDiscoveryCrawler, incrementalCrawler);

        assertThat(orchestrator.runIncrementalRefresh()).isEqualTo(expected);
        verify(incrementalCrawler).crawlEnabledSubcategories();
    }
}
