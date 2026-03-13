package md.hashcode.vector9.client;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import md.hashcode.vector9.model.graphql.AdSubcategoryUrlData;
import md.hashcode.vector9.model.graphql.AdViewsData;
import md.hashcode.vector9.model.graphql.GraphqlError;
import md.hashcode.vector9.model.graphql.GraphqlResponse;
import md.hashcode.vector9.model.graphql.SearchAdsData;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Service
public class GraphqlClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Autowired
    public GraphqlClient(OkHttpClient okHttpClient, ObjectMapper objectMapper, GraphqlProperties properties) {
        this(okHttpClient, objectMapper, properties.getBaseUrl());
    }

    public GraphqlClient(OkHttpClient okHttpClient, ObjectMapper objectMapper, String baseUrl) {
        this.okHttpClient = Objects.requireNonNull(okHttpClient, "okHttpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    }

    public GraphqlResponse<SearchAdsData> searchAds(SearchAdsRequest request) {
        return execute(request.toGraphqlRequest(), SearchAdsData.class);
    }

    public GraphqlResponse<AdViewsData> adViews(AdViewsRequest request) {
        return execute(request.toGraphqlRequest(), AdViewsData.class);
    }

    public GraphqlResponse<AdSubcategoryUrlData> adSubcategoryUrl(AdSubcategoryUrlRequest request) {
        return execute(request.toGraphqlRequest(), AdSubcategoryUrlData.class);
    }

    public <T> GraphqlResponse<T> execute(GraphqlRequest<?> graphqlRequest, Class<T> dataType) {
        Objects.requireNonNull(graphqlRequest, "graphqlRequest");
        Objects.requireNonNull(dataType, "dataType");

        Request request = buildHttpRequest(graphqlRequest);
        JavaType responseType = objectMapper.getTypeFactory()
                .constructParametricType(GraphqlResponse.class, dataType);

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new GraphqlClientException("GraphQL HTTP failure for operation %s: %s".formatted(
                        graphqlRequest.operationName(),
                        response.code()
                ));
            }

            if (response.body() == null) {
                throw new GraphqlClientException("GraphQL response body was empty for operation " + graphqlRequest.operationName());
            }

            GraphqlResponse<T> parsed = objectMapper.readValue(response.body().byteStream(), responseType);
            if (parsed.errors() != null && !parsed.errors().isEmpty()) {
                throw new GraphqlClientException(formatErrors(graphqlRequest.operationName(), parsed.errors()));
            }

            return parsed;
        } catch (IOException e) {
            throw new GraphqlClientException("GraphQL request failed for operation " + graphqlRequest.operationName(), e);
        }
    }

    private Request buildHttpRequest(GraphqlRequest<?> graphqlRequest) {
        String body = objectMapper.writeValueAsString(graphqlRequest);

        return new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE))
                .header("content-type", "application/json")
                .header("accept", "*/*")
                .header("lang", graphqlRequest.language())
                .header("source", graphqlRequest.sourceHeader())
                .build();
    }

    private String formatErrors(String operationName, List<GraphqlError> errors) {
        String details = errors.stream()
                .map(GraphqlError::message)
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + "; " + right)
                .orElse("unknown GraphQL error");
        return "GraphQL semantic failure for operation %s: %s".formatted(operationName, details);
    }
}