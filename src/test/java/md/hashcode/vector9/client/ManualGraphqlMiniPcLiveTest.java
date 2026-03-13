package md.hashcode.vector9.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import md.hashcode.vector9.model.ProcessedAdCommand;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@Disabled("Manual live test. Run explicitly when network access is available.")
class ManualGraphqlMiniPcLiveTest {

    @Test
    void shouldFetchMiniPcAdsAndWriteMappedEntitiesToJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        GraphqlClient client = new GraphqlClient(
                new OkHttpClient(),
                objectMapper,
                "https://999.md/graphql"
        );
        GraphqlAdMapper mapper = new GraphqlAdMapper();

        SearchAdsRequest request = SearchAdsRequest.builder()
                .subcategoryId(7661L)
                .limit(10)
                .skip(0)
                .includeOwner(true)
                .addFilter(16L, 1L, List.of(776L));

        var response = client.searchAds(request);
        List<ProcessedAdCommand> mapped = mapper.toProcessedAds(response.data().searchAds().ads());

        Path outputDir = Path.of("target", "manual-output");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("mini-pc-processed-ads.json");
        Files.writeString(outputFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapped));

        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapped));
        System.out.println("Wrote manual GraphQL output to " + outputFile.toAbsolutePath());
    }
}