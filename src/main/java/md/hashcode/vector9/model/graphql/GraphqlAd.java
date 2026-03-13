package md.hashcode.vector9.model.graphql;

public record GraphqlAd(
        String id,
        String title,
        GraphqlCategory subCategory,
        GraphqlFeatureValue price,
        GraphqlFeatureValue pricePerMeter,
        GraphqlFeatureValue oldPrice,
        GraphqlFeatureValue images,
        GraphqlOwner owner,
        GraphqlFeatureValue transportYear,
        GraphqlFeatureValue realEstate,
        GraphqlFeatureValue body,
        GraphqlDisplayLabel label,
        GraphqlDisplayToggle frame,
        GraphqlDisplayToggle animation,
        GraphqlDisplayToggle animationAndFrame,
        String reseted
) {
}
