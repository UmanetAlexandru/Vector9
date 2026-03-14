package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.LocalDateTime;

import md.hashcode.vector9.crawler.CrawlBatchResult;
import md.hashcode.vector9.repository.JobExecutionStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionTracker {

    public static final String JOB_INCREMENTAL_CRAWLER = "incremental-crawler";
    public static final String JOB_INITIAL_DISCOVERY = "initial-discovery-crawler";
    public static final String JOB_DELETION_DETECTION = "deletion-detection";
    public static final String JOB_VIEW_TRACKING = "view-tracking";
    public static final String JOB_DETAIL_ENRICHMENT = "detail-enrichment";

    private final JobExecutionStateRepository repository;
    private final OperationalMetricsRecorder metricsRecorder;
    private final Clock clock;

    @Autowired
    public JobExecutionTracker(JobExecutionStateRepository repository,
                               OperationalMetricsRecorder metricsRecorder) {
        this(repository, metricsRecorder, Clock.systemDefaultZone());
    }

    public JobExecutionTracker(JobExecutionStateRepository repository,
                               OperationalMetricsRecorder metricsRecorder,
                               Clock clock) {
        this.repository = repository;
        this.metricsRecorder = metricsRecorder;
        this.clock = clock;
    }

    public void recordCrawlerBatch(String jobName, CrawlBatchResult result, long durationMs) {
        int adsProcessed = result.results().stream().mapToInt(subcategory -> subcategory.adsProcessed()).sum();
        metricsRecorder.recordAdsProcessed(jobName, adsProcessed);
        boolean success = result.failures() == 0;
        if (success) {
            recordSuccess(jobName, durationMs);
        } else {
            recordFailure(jobName, durationMs, result.failures() + " subcategory failures");
        }
    }

    public void recordDeletionResult(DeletionJobResult result, long durationMs) {
        metricsRecorder.recordAdsMarkedDeleted(result.adsMarkedDeleted());
        if (result.failureMessage() == null) {
            recordSuccess(JOB_DELETION_DETECTION, durationMs);
        } else {
            recordFailure(JOB_DELETION_DETECTION, durationMs, result.failureMessage());
        }
    }

    public void recordViewTrackingResult(ViewTrackingJobResult result, long durationMs) {
        metricsRecorder.recordViewTrackingUpdates(result.adsUpdated(), result.historyRowsInserted());
        if (result.batchesFailed() == 0) {
            recordSuccess(JOB_VIEW_TRACKING, durationMs);
        } else {
            recordFailure(JOB_VIEW_TRACKING, durationMs, String.join("; ", result.failureMessages()));
        }
    }

    public void recordEnrichmentResult(EnrichmentJobResult result, long durationMs) {
        metricsRecorder.recordDetailEnrichment(result.adsEnriched(), result.adsFailed());
        if (result.adsFailed() == 0) {
            recordSuccess(JOB_DETAIL_ENRICHMENT, durationMs);
        } else {
            recordFailure(JOB_DETAIL_ENRICHMENT, durationMs, String.join("; ", result.failureMessages()));
        }
    }

    public void recordSuccess(String jobName, long durationMs) {
        LocalDateTime now = LocalDateTime.now(clock);
        metricsRecorder.recordJobExecution(jobName, true, durationMs);
        repository.upsertSuccess(jobName, durationMs, now);
    }

    public void recordFailure(String jobName, long durationMs, String errorMessage) {
        LocalDateTime now = LocalDateTime.now(clock);
        metricsRecorder.recordJobExecution(jobName, false, durationMs);
        repository.upsertFailure(jobName, durationMs, errorMessage, now);
    }
}
