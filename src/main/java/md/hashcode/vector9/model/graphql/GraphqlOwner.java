package md.hashcode.vector9.model.graphql;

public record GraphqlOwner(
        String id,
        String login,
        String avatar,
        String createdDate,
        GraphqlBusiness business,
        GraphqlVerification verification
) {
}
