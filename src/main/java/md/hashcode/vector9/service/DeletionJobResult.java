package md.hashcode.vector9.service;

public record DeletionJobResult(
        int adsChecked,
        int adsMarkedDeleted,
        int thresholdDays,
        String failureMessage
) {
}
