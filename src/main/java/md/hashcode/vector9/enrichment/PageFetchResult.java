package md.hashcode.vector9.enrichment;

public record PageFetchResult(
        String url,
        String html,
        long durationMs
) {
}
