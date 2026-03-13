package md.hashcode.vector9.model.graphql;

public record GraphqlDisplayLabel(
        Boolean enable,
        String title,
        GraphqlColor color,
        GraphqlGradient gradient
) {
}
