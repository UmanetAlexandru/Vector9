package md.hashcode.vector9.crawler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import md.hashcode.vector9.client.GraphqlAdMapper;
import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.CrawlCheckpointsRecord;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import md.hashcode.vector9.model.CheckpointUpdateCommand;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.ProcessedAdResult;
import md.hashcode.vector9.model.graphql.SearchAdsData;
import md.hashcode.vector9.model.graphql.SearchAdsResult;
import md.hashcode.vector9.repository.CrawlCheckpointRepository;
import md.hashcode.vector9.repository.SubcategoryRepository;
import md.hashcode.vector9.service.AdPersistenceService;
import md.hashcode.vector9.service.JobExecutionTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitialDiscoveryCrawler {

    private final SubcategoryRepository subcategoryRepository;
    private final CrawlCheckpointRepository crawlCheckpointRepository;
    private final GraphqlClient graphqlClient;
    private final GraphqlAdMapper graphqlAdMapper;
    private final AdPersistenceService adPersistenceService;
    private final SearchAdsRequestFactory requestFactory;
    private final CrawlerProperties crawlerProperties;
    private JobExecutionTracker jobExecutionTracker;

    public InitialDiscoveryCrawler(
            SubcategoryRepository subcategoryRepository,
            CrawlCheckpointRepository crawlCheckpointRepository,
            GraphqlClient graphqlClient,
            GraphqlAdMapper graphqlAdMapper,
            AdPersistenceService adPersistenceService,
            SearchAdsRequestFactory requestFactory,
            CrawlerProperties crawlerProperties
    ) {
        this.subcategoryRepository = subcategoryRepository;
        this.crawlCheckpointRepository = crawlCheckpointRepository;
        this.graphqlClient = graphqlClient;
        this.graphqlAdMapper = graphqlAdMapper;
        this.adPersistenceService = adPersistenceService;
        this.requestFactory = requestFactory;
        this.crawlerProperties = crawlerProperties;
    }

    public CrawlBatchResult crawlEnabledSubcategories() {
        long startedAt = System.nanoTime();
        List<SubcategoryCrawlResult> results = new ArrayList<>();
        for (SubcategoriesRecord subcategory : subcategoryRepository.findEnabled()) {
            results.add(crawlSubcategory(subcategory));
        }
        CrawlBatchResult batchResult = CrawlBatchResult.fromResults(results);
        if (jobExecutionTracker != null) {
            jobExecutionTracker.recordCrawlerBatch(
                    JobExecutionTracker.JOB_INITIAL_DISCOVERY,
                    batchResult,
                    elapsedMillis(startedAt)
            );
        }
        return batchResult;
    }

    @Autowired(required = false)
    public void setJobExecutionTracker(JobExecutionTracker jobExecutionTracker) {
        this.jobExecutionTracker = jobExecutionTracker;
    }

    public SubcategoryCrawlResult crawlSubcategory(SubcategoriesRecord subcategory) {
        Objects.requireNonNull(subcategory, "subcategory");

        long subcategoryId = subcategory.getId();
        int pageSize = crawlerProperties.getPageSize();
        Optional<CrawlCheckpointsRecord> checkpoint = crawlCheckpointRepository.findBySubcategoryId(subcategoryId);
        int skip = checkpoint
                .map(record -> record.getCurrentSkip() != null ? record.getCurrentSkip() : 0)
                .orElse(0);
        int processedBeforeRun = checkpoint
                .map(record -> record.getAdsProcessed() != null ? record.getAdsProcessed() : 0)
                .orElse(0);

        int adsFetched = 0;
        int adsProcessed = 0;
        int newAds = 0;
        int updatedAds = 0;
        int unchangedAds = 0;
        int pagesFetched = 0;

        try {
            while (true) {
                SearchAdsRequest request = requestFactory.build(subcategory, skip);
                SearchAdsResult searchAds = requireSearchAds(graphqlClient.searchAds(request).data(), subcategoryId);
                List<ProcessedAdCommand> processedAds = graphqlAdMapper.toProcessedAds(searchAds.ads());

                if (processedAds.isEmpty()) {
                    crawlCheckpointRepository.clear(subcategoryId);
                    return new SubcategoryCrawlResult(
                            subcategoryId,
                            adsFetched,
                            adsProcessed,
                            newAds,
                            updatedAds,
                            unchangedAds,
                            pagesFetched,
                            true,
                            false,
                            null
                    );
                }

                pagesFetched++;
                adsFetched += processedAds.size();
                for (ProcessedAdCommand processedAd : processedAds) {
                    ProcessedAdResult result = adPersistenceService.persist(processedAd);
                    adsProcessed++;
                    if (result.created()) {
                        newAds++;
                    } else if (result.materiallyChanged()) {
                        updatedAds++;
                    } else {
                        unchangedAds++;
                    }
                }

                skip += processedAds.size();
                crawlCheckpointRepository.upsert(
                        new CheckpointUpdateCommand(
                                subcategoryId,
                                skip,
                                searchAds.count(),
                                processedBeforeRun + adsProcessed
                        ),
                        LocalDateTime.now()
                );

                if (processedAds.size() < pageSize) {
                    crawlCheckpointRepository.clear(subcategoryId);
                    return new SubcategoryCrawlResult(
                            subcategoryId,
                            adsFetched,
                            adsProcessed,
                            newAds,
                            updatedAds,
                            unchangedAds,
                            pagesFetched,
                            true,
                            false,
                            null
                    );
                }
            }
        } catch (RuntimeException exception) {
            return new SubcategoryCrawlResult(
                    subcategoryId,
                    adsFetched,
                    adsProcessed,
                    newAds,
                    updatedAds,
                    unchangedAds,
                    pagesFetched,
                    false,
                    false,
                    "Initial crawl failed for subcategory %d: %s".formatted(subcategoryId, exception.getMessage())
            );
        }
    }

    private SearchAdsResult requireSearchAds(SearchAdsData data, long subcategoryId) {
        if (data == null || data.searchAds() == null) {
            throw new IllegalStateException("Missing SearchAds payload for subcategory " + subcategoryId);
        }
        return data.searchAds();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
