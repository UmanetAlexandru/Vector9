package md.hashcode.vector9.model.graphql;

public record GraphqlGradient(
        GraphqlColor from,
        GraphqlColor to,
        Integer position,
        Integer rotation
) {
}
