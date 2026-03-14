package md.hashcode.vector9.service;

import java.util.List;

public record ViewTrackingJobResult(
        int adsRequested,
        int adsUpdated,
        int historyRowsInserted,
        int batchesAttempted,
        int batchesSucceeded,
        int batchesFailed,
        List<String> failureMessages
) {
}
