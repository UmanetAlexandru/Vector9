package md.hashcode.vector9.enrichment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AdDetailExtractorTest {

    private final AdDetailExtractor extractor = new AdDetailExtractor(new ObjectMapper());

    @Test
    void shouldExtractGroupedAndNormalizedDetailsFromEmbeddedAdView() throws IOException {
        String html = loadFixture("/fixtures/mini-pc-ad-103748724-embedded.html");

        ExtractedAdDetails result = extractor.extract("https://999.md/ro/103748724", html, 321L);

        assertThat(result.adId()).isEqualTo(103748724L);
        assertThat(result.sourceType()).isEqualTo("embedded_page_state");
        assertThat(result.title()).contains("MacMini M2 Pro");
        assertThat(result.body()).contains("Stare perfect");
        assertThat(result.scrapeDurationMs()).isEqualTo(321L);

        ExtractedAttribute ram = findAttribute(result.groupedAttributes(), "Memorie RAM");
        assertThat(ram.featureId()).isEqualTo(1244L);
        assertThat(ram.translatedValue()).isEqualTo("16 GB");

        assertThat(result.normalizedAttributes()).containsEntry("ram", "16 GB");
        assertThat(result.normalizedAttributes()).containsEntry("cpu_model", "M2 Pro");
        assertThat(result.normalizedAttributes()).containsEntry("cpu_series", "Apple");
        assertThat(result.normalizedAttributes()).containsEntry("storage_capacity", "512 GB");
        assertThat(result.normalizedAttributes()).containsEntry("operating_system", "MacOs");
        assertThat(result.normalizedAttributes()).containsEntry("power_watts", 220);
        assertThat(result.normalizedAttributes()).containsEntry("region", "Chișinău mun.");
        assertThat(result.normalizedAttributes().get("phone_numbers")).isEqualTo(List.of("37379944988"));

        assertThat(result.location()).containsEntry("region", "Chișinău mun.");
        assertThat(result.contactInfo()).containsEntry("phoneNumbers", List.of("37379944988"));
        assertThat(result.contactInfo()).containsKey("owner");
        assertThat(result.contactInfo()).containsKey("quickReplies");
        assertThat(result.characteristicsPayload()).containsEntry("sourceType", "embedded_page_state");
    }

    private ExtractedAttribute findAttribute(List<ExtractedAttributeGroup> groups, String title) {
        return groups.stream()
                .flatMap(group -> group.attributes().stream())
                .filter(attribute -> title.equals(attribute.title()))
                .findFirst()
                .orElseThrow();
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
