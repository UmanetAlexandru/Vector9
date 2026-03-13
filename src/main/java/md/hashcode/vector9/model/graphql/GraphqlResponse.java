package md.hashcode.vector9.model.graphql;

import java.util.List;

public record GraphqlResponse<T>(
        T data,
        List<GraphqlError> errors
) {
}
