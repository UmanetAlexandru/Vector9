package md.hashcode.vector9.enrichment;

public class EnrichmentException extends RuntimeException {

    private final boolean retryable;

    public EnrichmentException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public EnrichmentException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
