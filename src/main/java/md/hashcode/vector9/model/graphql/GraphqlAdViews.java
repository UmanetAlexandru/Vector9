package md.hashcode.vector9.model.graphql;

public record GraphqlAdViews(
        Long adId,
        Integer viewsToday,
        Integer viewsTotal,
        Integer viewsSinceRepublish
) {
}
