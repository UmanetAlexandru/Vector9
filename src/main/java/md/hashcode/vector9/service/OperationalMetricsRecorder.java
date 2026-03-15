package md.hashcode.vector9.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OperationalMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public OperationalMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordGraphqlRequest(String operationName, boolean success, long durationMs) {
        String status = success ? "success" : "failure";
        meterRegistry.counter("vector9.graphql.requests", "operation", operationName, "status", status)
                .increment();
        Timer.builder("vector9.graphql.request.duration")
                .tag("operation", operationName)
                .tag("status", status)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void recordJobExecution(String jobName, boolean success, long durationMs) {
        String status = success ? "success" : "failure";
        meterRegistry.counter("vector9.jobs.executions", "job", jobName, "status", status)
                .increment();
        Timer.builder("vector9.jobs.execution.duration")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void recordAdsProcessed(String jobName, int count) {
        if (count > 0) {
            meterRegistry.counter("vector9.ads.processed", "job", jobName).increment(count);
        }
    }

    public void recordAdsMarkedDeleted(int count) {
        if (count > 0) {
            meterRegistry.counter("vector9.ads.deleted").increment(count);
        }
    }

    public void recordViewTrackingUpdates(int adsUpdated, int historyRowsInserted) {
        if (adsUpdated > 0) {
            meterRegistry.counter("vector9.views.updated_ads").increment(adsUpdated);
        }
        if (historyRowsInserted > 0) {
            meterRegistry.counter("vector9.views.history_rows").increment(historyRowsInserted);
        }
    }

    public void recordDetailEnrichment(int adsEnriched, int adsFailed) {
        if (adsEnriched > 0) {
            meterRegistry.counter("vector9.enrichment.enriched_ads").increment(adsEnriched);
        }
        if (adsFailed > 0) {
            meterRegistry.counter("vector9.enrichment.failed_ads").increment(adsFailed);
        }
    }

    public void recordDownstreamEventCreated(String eventType) {
        meterRegistry.counter("vector9.downstream.events.created", "event_type", eventType).increment();
    }

    public void recordDownstreamDelivery(String eventType, boolean success) {
        meterRegistry.counter(
                "vector9.downstream.deliveries",
                "event_type", eventType,
                "status", success ? "success" : "failure"
        ).increment();
    }
}
