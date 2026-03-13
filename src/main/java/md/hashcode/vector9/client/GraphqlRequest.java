package md.hashcode.vector9.client;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record GraphqlRequest<T>(
        String operationName,
        T variables,
        String query,
        @JsonIgnore String language,
        @JsonIgnore String sourceHeader
) {

    public GraphqlRequest {
        language = language != null ? language : "ro";
        sourceHeader = sourceHeader != null ? sourceHeader : "desktop";
    }
}
