package md.hashcode.vector9.model.graphql;

import java.util.List;

public record GraphqlError(
        String message,
        List<Object> path
) {
}
