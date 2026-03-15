package md.hashcode.vector9.notification;

import java.util.List;

import md.hashcode.vector9.service.JobExecutionTracker;
import md.hashcode.vector9.service.ObservabilityProperties;
import org.springframework.stereotype.Component;

@Component
public class MonitoredJobCatalog {

    private final ObservabilityProperties observabilityProperties;

    public MonitoredJobCatalog(ObservabilityProperties observabilityProperties) {
        this.observabilityProperties = observabilityProperties;
    }

    public List<MonitoredJobDefinition> expectedJobs() {
        return List.of(
                new MonitoredJobDefinition(
                        JobExecutionTracker.JOB_INCREMENTAL_CRAWLER,
                        "Incremental crawler",
                        observabilityProperties.getIncrementalCrawlerFreshnessMinutes()
                ),
                new MonitoredJobDefinition(
                        JobExecutionTracker.JOB_DELETION_DETECTION,
                        "Deletion detection",
                        observabilityProperties.getDeletionDetectionFreshnessMinutes()
                ),
                new MonitoredJobDefinition(
                        JobExecutionTracker.JOB_VIEW_TRACKING,
                        "View tracking",
                        observabilityProperties.getViewTrackingFreshnessMinutes()
                ),
                new MonitoredJobDefinition(
                        JobExecutionTracker.JOB_DETAIL_ENRICHMENT,
                        "Detail enrichment",
                        observabilityProperties.getDetailEnrichmentFreshnessMinutes()
                )
        );
    }
}
