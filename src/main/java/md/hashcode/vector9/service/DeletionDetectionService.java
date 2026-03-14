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
        int thresholdDays = trackingProperties.getDeletionThresholdDays();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(thresholdDays);

        try {
            var staleAds = adRepository.findActiveAdsLastSeenBefore(cutoff);
            int updated = adRepository.markDeleted(
                    staleAds.stream().map(ad -> ad.getId()).toList(),
                    now
            );
            return new DeletionJobResult(staleAds.size(), updated, thresholdDays, null);
        } catch (RuntimeException exception) {
            return new DeletionJobResult(0, 0, thresholdDays, exception.getMessage());
        }
    }
}
