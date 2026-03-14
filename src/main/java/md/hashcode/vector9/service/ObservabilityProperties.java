package md.hashcode.vector9.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vector9.observability")
public class ObservabilityProperties {

    private long incrementalCrawlerFreshnessMinutes = 30;
    private long deletionDetectionFreshnessMinutes = 180;
    private long viewTrackingFreshnessMinutes = 180;

    public long getIncrementalCrawlerFreshnessMinutes() {
        return incrementalCrawlerFreshnessMinutes;
    }

    public void setIncrementalCrawlerFreshnessMinutes(long incrementalCrawlerFreshnessMinutes) {
        this.incrementalCrawlerFreshnessMinutes = incrementalCrawlerFreshnessMinutes;
    }

    public long getDeletionDetectionFreshnessMinutes() {
        return deletionDetectionFreshnessMinutes;
    }

    public void setDeletionDetectionFreshnessMinutes(long deletionDetectionFreshnessMinutes) {
        this.deletionDetectionFreshnessMinutes = deletionDetectionFreshnessMinutes;
    }

    public long getViewTrackingFreshnessMinutes() {
        return viewTrackingFreshnessMinutes;
    }

    public void setViewTrackingFreshnessMinutes(long viewTrackingFreshnessMinutes) {
        this.viewTrackingFreshnessMinutes = viewTrackingFreshnessMinutes;
    }
}
