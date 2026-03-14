package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import md.hashcode.vector9.enrichment.AdDetailExtractor;
import md.hashcode.vector9.enrichment.DetailPageFetcher;
import md.hashcode.vector9.enrichment.EnrichmentException;
import md.hashcode.vector9.enrichment.EnrichmentProperties;
import md.hashcode.vector9.repository.AdAttributesRepository;
import md.hashcode.vector9.repository.AdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DetailEnrichmentService {

    private final AdRepository adRepository;
    private final AdAttributesRepository adAttributesRepository;
    private final DetailPageFetcher detailPageFetcher;
    private final AdDetailExtractor adDetailExtractor;
    private final EnrichmentProperties enrichmentProperties;
    private final Clock clock;
    private JobExecutionTracker jobExecutionTracker;

    @Autowired
    public DetailEnrichmentService(AdRepository adRepository,
                                   AdAttributesRepository adAttributesRepository,
                                   DetailPageFetcher detailPageFetcher,
                                   AdDetailExtractor adDetailExtractor,
                                   EnrichmentProperties enrichmentProperties) {
        this(adRepository, adAttributesRepository, detailPageFetcher, adDetailExtractor, enrichmentProperties, Clock.systemDefaultZone());
    }

    public DetailEnrichmentService(AdRepository adRepository,
                                   AdAttributesRepository adAttributesRepository,
                                   DetailPageFetcher detailPageFetcher,
                                   AdDetailExtractor adDetailExtractor,
                                   EnrichmentProperties enrichmentProperties,
                                   Clock clock) {
        this.adRepository = adRepository;
        this.adAttributesRepository = adAttributesRepository;
        this.detailPageFetcher = detailPageFetcher;
        this.adDetailExtractor = adDetailExtractor;
        this.enrichmentProperties = enrichmentProperties;
        this.clock = clock;
    }

    public EnrichmentJobResult enrichPendingAds() {
        long startedAt = System.nanoTime();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime refreshCutoff = now.minusDays(enrichmentProperties.getRefreshDays());
        var candidates = adRepository.findCandidatesForEnrichment(
                refreshCutoff,
                enrichmentProperties.getRetryLimit(),
                enrichmentProperties.getBatchSize()
        );

        int adsAttempted = 0;
        int adsEnriched = 0;
        int adsFailed = 0;
        int adsSkipped = 0;
        int retryableFailures = 0;
        List<String> failureMessages = new ArrayList<>();

        for (var candidate : candidates) {
            if (candidate.getId() == null) {
                adsSkipped++;
                continue;
            }

            LocalDateTime attemptAt = LocalDateTime.now(clock);
            if (adRepository.markEnrichmentStarted(candidate.getId(), attemptAt) == 0) {
                adsSkipped++;
                continue;
            }
            adsAttempted++;

            try {
                String url = buildAdUrl(candidate.getId());
                var pageFetchResult = detailPageFetcher.fetch(url);
                var extracted = adDetailExtractor.extract(url, pageFetchResult.html(), pageFetchResult.durationMs());

                adAttributesRepository.upsert(
                        extracted.adId(),
                        extracted.characteristicsPayload(),
                        extracted.location(),
                        extracted.contactInfo(),
                        attemptAt,
                        Math.toIntExact(extracted.scrapeDurationMs())
                );
                adRepository.markEnrichmentSucceeded(candidate.getId(), attemptAt);
                adsEnriched++;
            } catch (RuntimeException exception) {
                boolean retryable = isRetryable(exception);
                adRepository.markEnrichmentFailed(candidate.getId(), attemptAt, retryable);
                if (retryable) {
                    retryableFailures++;
                }
                adsFailed++;
                failureMessages.add("Enrichment failed for ad %s: %s".formatted(candidate.getId(), exception.getMessage()));
            }

            applyDelayIfNeeded();
        }

        EnrichmentJobResult result = new EnrichmentJobResult(
                candidates.size(),
                adsAttempted,
                adsEnriched,
                adsFailed,
                adsSkipped,
                retryableFailures,
                List.copyOf(failureMessages)
        );
        if (jobExecutionTracker != null) {
            jobExecutionTracker.recordEnrichmentResult(result, elapsedMillis(startedAt));
        }
        return result;
    }

    private String buildAdUrl(long adId) {
        String baseUrl = enrichmentProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + enrichmentProperties.getLanguage() + "/" + adId;
    }

    private boolean isRetryable(RuntimeException exception) {
        if (exception instanceof EnrichmentException enrichmentException) {
            return enrichmentException.isRetryable();
        }
        return true;
    }

    private void applyDelayIfNeeded() {
        if (enrichmentProperties.getDelayMs() <= 0) {
            return;
        }

        try {
            Thread.sleep(enrichmentProperties.getDelayMs());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EnrichmentException("Enrichment execution was interrupted", exception, true);
        }
    }

    @Autowired(required = false)
    public void setJobExecutionTracker(JobExecutionTracker jobExecutionTracker) {
        this.jobExecutionTracker = jobExecutionTracker;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}