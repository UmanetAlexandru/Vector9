package md.hashcode.vector9.crawler;

public record SubcategoryCrawlResult(
        long subcategoryId,
        int adsFetched,
        int adsProcessed,
        int newAds,
        int updatedAds,
        int unchangedAds,
        int pagesFetched,
        boolean completed,
        boolean stoppedEarly,
        String failureMessage
) {

    public boolean successful() {
        return failureMessage == null;
    }
}