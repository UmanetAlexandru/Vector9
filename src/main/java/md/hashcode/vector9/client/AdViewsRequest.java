package md.hashcode.vector9.client;

import java.util.List;
import java.util.Objects;

public final class AdViewsRequest {

    private List<Long> adIds = List.of();
    private String language = "ro";
    private String sourceHeader = "desktop";

    private AdViewsRequest() {
    }

    public static AdViewsRequest builder() {
        return new AdViewsRequest();
    }

    public AdViewsRequest adIds(List<Long> adIds) {
        this.adIds = List.copyOf(adIds);
        return this;
    }

    public AdViewsRequest language(String language) {
        this.language = language;
        return this;
    }

    public AdViewsRequest sourceHeader(String sourceHeader) {
        this.sourceHeader = sourceHeader;
        return this;
    }

    public GraphqlRequest<AdViewsVariables> toGraphqlRequest() {
        if (adIds.isEmpty()) {
            throw new IllegalArgumentException("adIds must not be empty");
        }

        return new GraphqlRequest<>(
                "AdViews",
                new AdViewsVariables(new AdViewsInput(adIds)),
                GraphqlQueries.AD_VIEWS,
                language,
                sourceHeader
        );
    }

    public record AdViewsVariables(AdViewsInput input) {
    }

    public record AdViewsInput(List<Long> adIds) {
        public AdViewsInput {
            adIds = Objects.requireNonNull(adIds, "adIds");
        }
    }
}
