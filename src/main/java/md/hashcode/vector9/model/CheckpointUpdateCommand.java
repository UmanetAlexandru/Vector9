package md.hashcode.vector9.model;

public record CheckpointUpdateCommand(
        long subcategoryId,
        int currentSkip,
        Integer totalAdsCount,
        int adsProcessed
) {
}