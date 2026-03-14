package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.LocalDateTime;

import md.hashcode.vector9.repository.AdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeletionDetectionService {

    private final AdRepository adRepository;
    private final TrackingProperties trackingProperties;
    private final Clock clock;
    private JobExecutionTracker jobExecutionTracker;

    @Autowired
    public DeletionDetectionService(AdRepository adRepository, TrackingProperties trackingProperties) {
        this(adRepository, trackingProperties, Clock.systemDefaultZone());
    }

    public DeletionDetectionService(AdRepository adRepository, TrackingProperties trackingProperties, Clock clock) {
        this.adRepository = adRepository;
        this.trackingProperties = trackingProperties;
        this.clock = clock;
    }

    public DeletionJobResult markStaleAdsDeleted() {
        long startedAt = System.nanoTime();
        int thresholdDays = trackingProperties.getDeletionThresholdDays();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(thresholdDays);

        try {
            var staleAds = adRepository.findActiveAdsLastSeenBefore(cutoff);
            int updated = adRepository.markDeleted(
                    staleAds.stream().map(ad -> ad.getId()).toList(),
                    now
            );
            DeletionJobResult result = new DeletionJobResult(staleAds.size(), updated, thresholdDays, null);
            if (jobExecutionTracker != null) {
                jobExecutionTracker.recordDeletionResult(result, elapsedMillis(startedAt));
            }
            return result;
        } catch (RuntimeException exception) {
            DeletionJobResult result = new DeletionJobResult(0, 0, thresholdDays, exception.getMessage());
            if (jobExecutionTracker != null) {
                jobExecutionTracker.recordDeletionResult(result, elapsedMillis(startedAt));
            }
            return result;
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
