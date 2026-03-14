package md.hashcode.vector9.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import md.hashcode.vector9.enrichment.AdDetailExtractor;
import md.hashcode.vector9.enrichment.DetailPageFetcher;
import md.hashcode.vector9.enrichment.EnrichmentProperties;
import md.hashcode.vector9.enrichment.PageFetchResult;
import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.repository.AdAttributesRepository;
import md.hashcode.vector9.service.DetailEnrichmentService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static md.hashcode.vector9.jooq.Tables.AD_ATTRIBUTES;
import static md.hashcode.vector9.jooq.Tables.ADS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DetailEnrichmentPersistenceIntegrationTest extends RepositoryIntegrationTestSupport {

    @Test
    void shouldPersistExtractedAttributesAndMarkAdAsEnriched() throws IOException {
        LocalDateTime lastSeenAt = LocalDateTime.of(2026, 3, 14, 12, 0);
        adRepository.upsert(adCommand(103748724L, lastSeenAt), null, lastSeenAt);

        String html = loadFixture("/fixtures/mini-pc-ad-103748724-embedded.html");
        DetailPageFetcher fetcher = mock(DetailPageFetcher.class);
        when(fetcher.fetch("https://999.md/ro/103748724"))
                .thenReturn(new PageFetchResult("https://999.md/ro/103748724", html, 432L));

        EnrichmentProperties properties = new EnrichmentProperties();
        properties.setBatchSize(10);
        properties.setDelayMs(0);
        properties.setRetryLimit(3);
        properties.setRefreshDays(30);
        properties.setBaseUrl("https://999.md");
        properties.setLanguage("ro");

        ObjectMapper objectMapper = new ObjectMapper();
        DetailEnrichmentService service = new DetailEnrichmentService(
                adRepository,
                new AdAttributesRepository(dslContext, objectMapper),
                fetcher,
                new AdDetailExtractor(objectMapper),
                properties,
                Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC)
        );

        var result = service.enrichPendingAds();

        assertThat(result.adsSelected()).isEqualTo(1);
        assertThat(result.adsAttempted()).isEqualTo(1);
        assertThat(result.adsEnriched()).isEqualTo(1);
        assertThat(result.adsFailed()).isZero();

        Boolean detailsEnriched = dslContext.select(ADS.DETAILS_ENRICHED)
                .from(ADS)
                .where(ADS.ID.eq(103748724L))
                .fetchOne(ADS.DETAILS_ENRICHED);
        String enrichmentStatus = dslContext.select(ADS.ENRICHMENT_STATUS)
                .from(ADS)
                .where(ADS.ID.eq(103748724L))
                .fetchOne(ADS.ENRICHMENT_STATUS);
        LocalDateTime detailsLastEnrichedAt = dslContext.select(ADS.DETAILS_LAST_ENRICHED_AT)
                .from(ADS)
                .where(ADS.ID.eq(103748724L))
                .fetchOne(ADS.DETAILS_LAST_ENRICHED_AT);

        assertThat(detailsEnriched).isTrue();
        assertThat(enrichmentStatus).isEqualTo("completed");
        assertThat(detailsLastEnrichedAt).isEqualTo(LocalDateTime.of(2026, 3, 14, 12, 0));

        var attributesRecord = dslContext.selectFrom(AD_ATTRIBUTES)
                .where(AD_ATTRIBUTES.AD_ID.eq(103748724L))
                .fetchOne();
        assertThat(attributesRecord).isNotNull();
        assertThat(attributesRecord.getScrapeDurationMs()).isEqualTo(432);

        var characteristics = objectMapper.readTree(attributesRecord.getCharacteristics().data());
        var location = objectMapper.readTree(attributesRecord.getLocation().data());
        var contactInfo = objectMapper.readTree(attributesRecord.getContactInfo().data());

        assertThat(characteristics.path("sourceType").asText()).isEqualTo("embedded_page_state");
        assertThat(characteristics.path("normalized").path("ram").asText()).isEqualTo("16 GB");
        assertThat(characteristics.path("normalized").path("cpu_model").asText()).isEqualTo("M2 Pro");
        assertThat(location.path("region").asText()).isEqualTo("Chișinău mun.");
        assertThat(contactInfo.path("phoneNumbers").get(0).asText()).isEqualTo("37379944988");
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private AdUpsertCommand adCommand(long id, LocalDateTime lastSeenAt) {
        return new AdUpsertCommand(
                id,
                "tracked-mini-pc",
                7661L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                null,
                null,
                null,
                "active",
                lastSeenAt
        );
    }
}