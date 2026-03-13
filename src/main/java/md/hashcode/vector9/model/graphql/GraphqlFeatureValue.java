package md.hashcode.vector9.model.graphql;

import tools.jackson.databind.JsonNode;

public record GraphqlFeatureValue(
        Long id,
        String type,
        JsonNode value
) {
}
