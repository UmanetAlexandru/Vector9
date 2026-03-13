package md.hashcode.vector9.client;

public class GraphqlClientException extends RuntimeException {

    public GraphqlClientException(String message) {
        super(message);
    }

    public GraphqlClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
