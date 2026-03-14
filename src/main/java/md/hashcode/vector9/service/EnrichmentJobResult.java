package md.hashcode.vector9.service;

import java.util.List;

public record EnrichmentJobResult(
        int adsSelected,
        int adsAttempted,
        int adsEnriched,
        int adsFailed,
        int adsSkipped,
        int retryableFailures,
        List<String> failureMessages
) {
}
