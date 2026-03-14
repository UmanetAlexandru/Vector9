package md.hashcode.vector9.service;

import md.hashcode.vector9.crawler.CrawlBatchResult;
import md.hashcode.vector9.crawler.IncrementalCrawler;
import md.hashcode.vector9.crawler.InitialDiscoveryCrawler;
import org.springframework.stereotype.Service;

@Service
public class CrawlOrchestrator {

    private final InitialDiscoveryCrawler initialDiscoveryCrawler;
    private final IncrementalCrawler incrementalCrawler;

    public CrawlOrchestrator(InitialDiscoveryCrawler initialDiscoveryCrawler, IncrementalCrawler incrementalCrawler) {
        this.initialDiscoveryCrawler = initialDiscoveryCrawler;
        this.incrementalCrawler = incrementalCrawler;
    }

    public CrawlBatchResult runInitialDiscovery() {
        return initialDiscoveryCrawler.crawlEnabledSubcategories();
    }

    public CrawlBatchResult runIncrementalRefresh() {
        return incrementalCrawler.crawlEnabledSubcategories();
    }
}