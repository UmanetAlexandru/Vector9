package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import md.hashcode.vector9.client.AdViewsRequest;
import md.hashcode.vector9.client.GraphqlClient;
import md.hashcode.vector9.client.GraphqlClientException;
import md.hashcode.vector9.model.graphql.GraphqlAdViews;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.ViewHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ViewTrackingService {

    private final AdRepository adRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final GraphqlClient graphqlClient;
    private final TrackingProperties trackingProperties;
    private final Clock clock;

    @Autowired
    public ViewTrackingService(AdRepository adRepository,
                               ViewHistoryRepository viewHistoryRepository,
                               GraphqlClient graphqlClient,
                               TrackingProperties trackingProperties) {
        this(adRepository, viewHistoryRepository, graphqlClient, trackingProperties, Clock.systemDefaultZone());
    }

    public ViewTrackingService(AdRepository adRepository,
                               ViewHistoryRepository viewHistoryRepository,
                               GraphqlClient graphqlClient,
                               TrackingProperties trackingProperties,
                               Clock clock) {
        this.adRepository = adRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.graphqlClient = graphqlClient;
        this.trackingProperties = trackingProperties;
        this.clock = clock;
    }

    public ViewTrackingJobResult refreshActiveAdViews() {
        var activeAds = adRepository.findActiveAdsForViewTracking();
        int batchSize = Math.max(1, trackingProperties.getViewBatchSize());
        List<String> failures = new ArrayList<>();
        int adsUpdated = 0;
        int historyRowsInserted = 0;
        int batchesAttempted = 0;
        int batchesSucceeded = 0;
        int batchesFailed = 0;

        for (int index = 0; index < activeAds.size(); index += batchSize) {
            batchesAttempted++;

            var batch = activeAds.subList(index, Math.min(index + batchSize, activeAds.size()));
            var adIds = batch.stream().map(ad -> ad.getId()).toList();

            try {
                var response = graphqlClient.adViews(AdViewsRequest.builder()
                        .adIds(adIds)
                        .language(trackingProperties.getLanguage())
                        .sourceHeader(trackingProperties.getSourceHeader()));

                var views = response.data() != null && response.data().adViews() != null
                        ? response.data().adViews()
                        : List.<GraphqlAdViews>of();

                LocalDateTime fetchedAt = LocalDateTime.now(clock);
                for (GraphqlAdViews adViews : views) {
                    if (adViews.adId() == null || adViews.viewsTotal() == null || adViews.viewsSinceRepublish() == null) {
                        continue;
                    }

                    adsUpdated += adRepository.updateViewCounters(
                            adViews.adId(),
                            adViews.viewsToday(),
                            adViews.viewsTotal(),
                            adViews.viewsSinceRepublish(),
                            fetchedAt
                    );

                    if (trackingProperties.isWriteViewHistory()) {
                        viewHistoryRepository.insertSnapshot(
                                adViews.adId(),
                                adViews.viewsTotal(),
                                adViews.viewsSinceRepublish(),
                                fetchedAt
                        );
                        historyRowsInserted++;
                    }
                }

                batchesSucceeded++;
            } catch (GraphqlClientException exception) {
                batchesFailed++;
                failures.add("AdViews batch failed for ids %s: %s".formatted(adIds, exception.getMessage()));
            }
        }

        return new ViewTrackingJobResult(
                activeAds.size(),
                adsUpdated,
                historyRowsInserted,
                batchesAttempted,
                batchesSucceeded,
                batchesFailed,
                List.copyOf(failures)
        );
    }
}
