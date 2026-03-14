package md.hashcode.vector9.config;

import java.time.Clock;

import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionTracker;
import md.hashcode.vector9.service.ObservabilityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean("incrementalCrawlerFreshness")
    JobFreshnessHealthIndicator incrementalCrawlerFreshness(JobExecutionStateRepository repository,
                                                            ObservabilityProperties properties) {
        return new JobFreshnessHealthIndicator(
                repository,
                Clock.systemDefaultZone(),
                JobExecutionTracker.JOB_INCREMENTAL_CRAWLER,
                "Incremental crawler",
                properties.getIncrementalCrawlerFreshnessMinutes()
        );
    }

    @Bean("deletionDetectionFreshness")
    JobFreshnessHealthIndicator deletionDetectionFreshness(JobExecutionStateRepository repository,
                                                           ObservabilityProperties properties) {
        return new JobFreshnessHealthIndicator(
                repository,
                Clock.systemDefaultZone(),
                JobExecutionTracker.JOB_DELETION_DETECTION,
                "Deletion detection",
                properties.getDeletionDetectionFreshnessMinutes()
        );
    }

    @Bean("viewTrackingFreshness")
    JobFreshnessHealthIndicator viewTrackingFreshness(JobExecutionStateRepository repository,
                                                      ObservabilityProperties properties) {
        return new JobFreshnessHealthIndicator(
                repository,
                Clock.systemDefaultZone(),
                JobExecutionTracker.JOB_VIEW_TRACKING,
                "View tracking",
                properties.getViewTrackingFreshnessMinutes()
        );
    }
}
