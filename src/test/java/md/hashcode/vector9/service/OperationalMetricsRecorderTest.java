package md.hashcode.vector9.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalMetricsRecorderTest {

    @Test
    void shouldRecordGraphqlAndJobMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OperationalMetricsRecorder recorder = new OperationalMetricsRecorder(meterRegistry);

        recorder.recordGraphqlRequest("SearchAds", true, 120);
        recorder.recordJobExecution("incremental-crawler", true, 500);
        recorder.recordAdsProcessed("incremental-crawler", 12);
        recorder.recordAdsMarkedDeleted(3);
        recorder.recordViewTrackingUpdates(4, 2);

        assertThat(meterRegistry.get("vector9.graphql.requests").tag("operation", "SearchAds").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("vector9.jobs.executions").tag("job", "incremental-crawler").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("vector9.ads.processed").tag("job", "incremental-crawler").counter().count()).isEqualTo(12.0);
        assertThat(meterRegistry.get("vector9.ads.deleted").counter().count()).isEqualTo(3.0);
        assertThat(meterRegistry.get("vector9.views.updated_ads").counter().count()).isEqualTo(4.0);
        assertThat(meterRegistry.get("vector9.views.history_rows").counter().count()).isEqualTo(2.0);
    }
}
