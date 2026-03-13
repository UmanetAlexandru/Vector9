package md.hashcode.vector9.client;

public final class AdSubcategoryUrlRequest {

    private long adId;
    private String language = "ro";
    private String sourceHeader = "desktop";

    private AdSubcategoryUrlRequest() {
    }

    public static AdSubcategoryUrlRequest builder() {
        return new AdSubcategoryUrlRequest();
    }

    public AdSubcategoryUrlRequest adId(long adId) {
        this.adId = adId;
        return this;
    }

    public AdSubcategoryUrlRequest language(String language) {
        this.language = language;
        return this;
    }

    public AdSubcategoryUrlRequest sourceHeader(String sourceHeader) {
        this.sourceHeader = sourceHeader;
        return this;
    }

    public GraphqlRequest<AdSubcategoryUrlVariables> toGraphqlRequest() {
        if (adId <= 0) {
            throw new IllegalArgumentException("adId must be positive");
        }

        return new GraphqlRequest<>(
                "AdSubcategoryUrl",
                new AdSubcategoryUrlVariables(new AdSubcategoryUrlInput(adId)),
                GraphqlQueries.AD_SUBCATEGORY_URL,
                language,
                sourceHeader
        );
    }

    public record AdSubcategoryUrlVariables(AdSubcategoryUrlInput input) {
    }

    public record AdSubcategoryUrlInput(long adId) {
    }
}
