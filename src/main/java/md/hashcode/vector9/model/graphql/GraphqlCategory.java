package md.hashcode.vector9.model.graphql;

public record GraphqlCategory(
        long id,
        GraphqlTranslation title,
        GraphqlCategory parent
) {
}
