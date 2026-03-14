package md.hashcode.vector9.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import md.hashcode.vector9.client.GraphqlAdMapper;
import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.SearchAdsRequest;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import md.hashcode.vector9.model.ProcessedAdCommand;
import md.hashcode.vector9.model.ProcessedAdResult;
import md.hashcode.vector9.model.graphql.SearchAdsData;
import md.hashcode.vector9.model.graphql.SearchAdsResult;
import md.hashcode.vector9.repository.SubcategoryRepository;
import md.hashcode.vector9.service.AdPersistenceService;
import org.springframework.stereotype.Service;

@Service
public class IncrementalCrawler {

    private final SubcategoryRepository subcategoryRepository;
    private final GraphqlClient graphqlClient;
    private final GraphqlAdMapper graphqlAdMapper;
    private final AdPersistenceService adPersistenceService;
    private final SearchAdsRequestFactory requestFactory;
    private final CrawlerProperties crawlerProperties;

    public IncrementalCrawler(
            SubcategoryRepository subcategoryRepository,
            GraphqlClient graphqlClient,
            GraphqlAdMapper graphqlAdMapper,
            AdPersistenceService adPersistenceService,
            SearchAdsRequestFactory requestFactory,
            CrawlerProperties crawlerProperties
    ) {
        this.subcategoryRepository = subcategoryRepository;
        this.graphqlClient = graphqlClient;
        this.graphqlAdMapper = graphqlAdMapper;
        this.adPersistenceService = adPersistenceService;
        this.requestFactory = requestFactory;
        this.crawlerProperties = crawlerProperties;
    }

    public CrawlBatchResult crawlEnabledSubcategories() {
        List<SubcategoryCrawlResult> results = new ArrayList<>();
        for (SubcategoriesRecord subcategory : subcategoryRepository.findEnabled()) {
            results.add(crawlSubcategory(subcategory));
        }
        return CrawlBatchResult.fromResults(results);
    }

    public SubcategoryCrawlResult crawlSubcategory(SubcategoriesRecord subcategory) {
        Objects.requireNonNull(subcategory, "subcategory");

        long subcategoryId = subcategory.getId();
        int pageSize = crawlerProperties.getPageSize();
        int unchangedStopThreshold = crawlerProperties.getUnchangedStopThreshold();
        int skip = 0;
        int adsFetched = 0;
        int adsProcessed = 0;
        int newAds = 0;
        int updatedAds = 0;
        int unchangedAds = 0;
        int pagesFetched = 0;
        int consecutiveUnchanged = 0;

        try {
            while (true) {
                SearchAdsRequest request = requestFactory.build(subcategory, skip);
                SearchAdsResult searchAds = requireSearchAds(graphqlClient.searchAds(request).data(), subcategoryId);
                List<ProcessedAdCommand> processedAds = graphqlAdMapper.toProcessedAds(searchAds.ads());

                if (processedAds.isEmpty()) {
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
                        consecutiveUnchanged = 0;
                    } else if (result.materiallyChanged()) {
                        updatedAds++;
                        consecutiveUnchanged = 0;
                    } else {
                        unchangedAds++;
                        consecutiveUnchanged++;
                    }

                    if (consecutiveUnchanged >= unchangedStopThreshold) {
                        return new SubcategoryCrawlResult(
                                subcategoryId,
                                adsFetched,
                                adsProcessed,
                                newAds,
                                updatedAds,
                                unchangedAds,
                                pagesFetched,
                                false,
                                true,
                                null
                        );
                    }
                }

                if (processedAds.size() < pageSize) {
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

                skip += processedAds.size();
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
                    "Incremental crawl failed for subcategory %d: %s".formatted(subcategoryId, exception.getMessage())
            );
        }
    }

    private SearchAdsResult requireSearchAds(SearchAdsData data, long subcategoryId) {
        if (data == null || data.searchAds() == null) {
            throw new IllegalStateException("Missing SearchAds payload for subcategory " + subcategoryId);
        }
        return data.searchAds();
    }
}