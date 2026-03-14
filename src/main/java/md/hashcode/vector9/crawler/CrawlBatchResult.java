package md.hashcode.vector9.crawler;

import java.util.List;

public record CrawlBatchResult(
        int subcategoriesAttempted,
        int successes,
        int failures,
        List<SubcategoryCrawlResult> results
) {

    public static CrawlBatchResult fromResults(List<SubcategoryCrawlResult> results) {
        int successes = (int) results.stream()
                .filter(SubcategoryCrawlResult::successful)
                .count();
        return new CrawlBatchResult(
                results.size(),
                successes,
                results.size() - successes,
                List.copyOf(results)
        );
    }
}