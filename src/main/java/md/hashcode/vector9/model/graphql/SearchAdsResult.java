package md.hashcode.vector9.model.graphql;

import java.util.List;

public record SearchAdsResult(
        List<GraphqlAd> ads,
        Integer count,
        Long reseted
) {
}
