package md.hashcode.vector9.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SearchAdsRequest {

    private long subcategoryId;
    private int limit = 50;
    private int skip;
    private String locale = "ro_RO";
    private boolean includeOwner = true;
    private boolean includeBody;
    private boolean includeCarsFeatures;
    private boolean includeBoost;
    private boolean isWorkCategory;
    private String inputSource = "AD_SOURCE_DESKTOP";
    private String headerLanguage = "ro";
    private String headerSource = "desktop";
    private final List<FilterGroup> filters = new ArrayList<>();

    private SearchAdsRequest() {
    }

    public static SearchAdsRequest builder() {
        return new SearchAdsRequest();
    }

    public SearchAdsRequest subcategoryId(long subcategoryId) {
        this.subcategoryId = subcategoryId;
        return this;
    }

    public SearchAdsRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    public SearchAdsRequest skip(int skip) {
        this.skip = skip;
        return this;
    }

    public SearchAdsRequest locale(String locale) {
        this.locale = locale;
        return this;
    }

    public SearchAdsRequest includeOwner(boolean includeOwner) {
        this.includeOwner = includeOwner;
        return this;
    }

    public SearchAdsRequest includeBody(boolean includeBody) {
        this.includeBody = includeBody;
        return this;
    }

    public SearchAdsRequest includeCarsFeatures(boolean includeCarsFeatures) {
        this.includeCarsFeatures = includeCarsFeatures;
        return this;
    }

    public SearchAdsRequest includeBoost(boolean includeBoost) {
        this.includeBoost = includeBoost;
        return this;
    }

    public SearchAdsRequest isWorkCategory(boolean isWorkCategory) {
        this.isWorkCategory = isWorkCategory;
        return this;
    }

    public SearchAdsRequest inputSource(String inputSource) {
        this.inputSource = inputSource;
        return this;
    }

    public SearchAdsRequest headerLanguage(String headerLanguage) {
        this.headerLanguage = headerLanguage;
        return this;
    }

    public SearchAdsRequest headerSource(String headerSource) {
        this.headerSource = headerSource;
        return this;
    }

    public SearchAdsRequest addFilter(long filterId, long featureId, List<Long> optionIds) {
        filters.add(new FilterGroup(filterId, List.of(new FilterFeature(featureId, List.copyOf(optionIds)))));
        return this;
    }

    public GraphqlRequest<SearchAdsVariables> toGraphqlRequest() {
        if (subcategoryId <= 0) {
            throw new IllegalArgumentException("subcategoryId must be positive");
        }

        SearchAdsInput input = new SearchAdsInput(
                subcategoryId,
                inputSource,
                List.copyOf(filters),
                new Pagination(limit, skip)
        );

        SearchAdsVariables variables = new SearchAdsVariables(
                input,
                isWorkCategory,
                includeCarsFeatures,
                includeBody,
                includeOwner,
                includeBoost,
                locale
        );

        return new GraphqlRequest<>(
                "SearchAds",
                variables,
                GraphqlQueries.SEARCH_ADS,
                headerLanguage,
                headerSource
        );
    }

    public record SearchAdsVariables(
            SearchAdsInput input,
            boolean isWorkCategory,
            boolean includeCarsFeatures,
            boolean includeBody,
            boolean includeOwner,
            boolean includeBoost,
            String locale
    ) {
    }

    public record SearchAdsInput(
            long subCategoryId,
            String source,
            List<FilterGroup> filters,
            Pagination pagination
    ) {
        public SearchAdsInput {
            source = Objects.requireNonNull(source, "source");
            filters = filters != null ? filters : List.of();
            pagination = Objects.requireNonNull(pagination, "pagination");
        }
    }

    public record Pagination(int limit, int skip) {
    }

    public record FilterGroup(long filterId, List<FilterFeature> features) {
        public FilterGroup {
            features = Objects.requireNonNull(features, "features");
        }
    }

    public record FilterFeature(long featureId, List<Long> optionIds) {
        public FilterFeature {
            optionIds = Objects.requireNonNull(optionIds, "optionIds");
        }
    }
}
