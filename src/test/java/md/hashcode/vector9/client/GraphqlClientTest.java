package md.hashcode.vector9.client;

import java.io.IOException;
import java.util.List;

import md.hashcode.vector9.model.graphql.SearchAdsData;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphqlClientTest {

    private MockWebServer server;
    private GraphqlClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new GraphqlClient(
                new OkHttpClient(),
                new ObjectMapper(),
                server.url("/graphql").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldPostSearchAdsRequestWithExpectedHeadersAndParseResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""
                        {
                          "data": {
                            "searchAds": {
                              "ads": [
                                {
                                  "id": "103465316",
                                  "title": "Mini PC",
                                  "subCategory": {"id": 7661, "title": {"translated": "Mini PC"}, "parent": null},
                                  "price": {"id": 2, "type": "FEATURE_PRICE", "value": {"measurement": "UNIT_MDL", "mode": "PM_FIXED", "unit": "UNIT_MDL", "value": 1699, "bargain": false, "down_payment": 0}},
                                  "pricePerMeter": null,
                                  "oldPrice": null,
                                  "images": {"id": 14, "type": "FEATURE_IMAGES", "value": ["a.jpg"]},
                                  "owner": {"id": "b2f0fb96-772e-4e08-9894-654bd64907a1", "login": "seller", "avatar": "", "createdDate": "", "business": null, "verification": null},
                                  "transportYear": null,
                                  "realEstate": null,
                                  "body": null,
                                  "label": null,
                                  "frame": null,
                                  "animation": null,
                                  "animationAndFrame": null,
                                  "reseted": "13 mar. 2026, 20:37"
                                }
                              ],
                              "count": 269,
                              "reseted": 1773429709
                            }
                          }
                        }
                        """));

        SearchAdsRequest request = SearchAdsRequest.builder()
                .subcategoryId(7661L)
                .limit(78)
                .skip(0)
                .addFilter(16L, 1L, List.of(776L));

        var response = client.searchAds(request);
        RecordedRequest recorded = server.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/graphql");
        assertThat(recorded.getHeader("content-type")).startsWith("application/json");
        assertThat(recorded.getHeader("lang")).isEqualTo("ro");
        assertThat(recorded.getHeader("source")).isEqualTo("desktop");
        assertThat(recorded.getBody().readUtf8()).contains("\"operationName\":\"SearchAds\"");
        assertThat(response.data()).isNotNull();
        SearchAdsData data = response.data();
        assertThat(data.searchAds().count()).isEqualTo(269);
        assertThat(data.searchAds().ads()).hasSize(1);
        assertThat(data.searchAds().ads().getFirst().owner()).isNotNull();
    }

    @Test
    void shouldParseCountOnlySearchAdsResponse() {
        server.enqueue(new MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""
                        {"data":{"searchAds":{"ads":[],"count":269,"reseted":1773429709}}}
                        """));

        var response = client.searchAds(SearchAdsRequest.builder()
                .subcategoryId(7661L)
                .limit(0)
                .skip(0)
                .addFilter(16L, 1L, List.of(776L)));

        assertThat(response.data().searchAds().ads()).isEmpty();
        assertThat(response.data().searchAds().count()).isEqualTo(269);
    }

    @Test
    void shouldThrowOnGraphqlErrors() {
        server.enqueue(new MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""
                        {"errors":[{"message":"Bad filter"}]}
                        """));

        assertThatThrownBy(() -> client.searchAds(SearchAdsRequest.builder().subcategoryId(7661L)))
                .isInstanceOf(GraphqlClientException.class)
                .hasMessageContaining("GraphQL semantic failure")
                .hasMessageContaining("Bad filter");
    }

    @Test
    void shouldParseAdViewsResponse() {
        server.enqueue(new MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""
                        {"data":{"adViews":[{"adId":103465316,"viewsToday":4,"viewsTotal":120,"viewsSinceRepublish":12}]}}
                        """));

        var response = client.adViews(AdViewsRequest.builder().adIds(List.of(103465316L)));

        assertThat(response.data()).isNotNull();
        assertThat(response.data().adViews()).hasSize(1);
        assertThat(response.data().adViews().getFirst().viewsTotal()).isEqualTo(120);
    }

    @Test
    void shouldThrowOnNon200Response() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        assertThatThrownBy(() -> client.searchAds(SearchAdsRequest.builder().subcategoryId(7661L)))
                .isInstanceOf(GraphqlClientException.class)
                .hasMessageContaining("HTTP failure")
                .hasMessageContaining("SearchAds");
    }
}
