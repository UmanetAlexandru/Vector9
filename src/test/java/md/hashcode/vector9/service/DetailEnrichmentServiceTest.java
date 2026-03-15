package md.hashcode.vector9.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import md.hashcode.vector9.enrichment.AdDetailExtractor;
import md.hashcode.vector9.enrichment.DetailPageFetcher;
import md.hashcode.vector9.enrichment.EnrichmentException;
import md.hashcode.vector9.enrichment.EnrichmentProperties;
import md.hashcode.vector9.enrichment.ExtractedAdDetails;
import md.hashcode.vector9.enrichment.PageFetchResult;
import md.hashcode.vector9.jooq.tables.records.AdsRecord;
import md.hashcode.vector9.repository.AdAttributesRepository;
import md.hashcode.vector9.repository.AdRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetailEnrichmentServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private AdAttributesRepository adAttributesRepository;
    @Mock
    private DetailPageFetcher detailPageFetcher;
    @Mock
    private AdDetailExtractor adDetailExtractor;

    @Test
    void shouldEnrichSelectedAdsAndPersistAttributes() {
        EnrichmentProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);
        when(adRepository.findCandidatesForEnrichment(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(ad(103748724L)));
        when(adRepository.markEnrichmentStarted(anyLong(), any())).thenReturn(1);
        when(detailPageFetcher.fetch("https://999.md/ro/103748724"))
                .thenReturn(new PageFetchResult("https://999.md/ro/103748724", "<html></html>", 250L));
        when(adDetailExtractor.extract(any(), any(), anyLong()))
                .thenReturn(new ExtractedAdDetails(
                        103748724L,
                        "https://999.md/ro/103748724",
                        "Mini PC",
                        "Body",
                        "embedded_page_state",
                        List.of(),
                        java.util.Map.of("ram", "16 GB"),
                        java.util.Map.of("region", "ChiÈ™inÄƒu mun."),
                        java.util.Map.of("phoneNumbers", List.of("37379944988")),
                        250L
                ));

        DetailEnrichmentService service = new DetailEnrichmentService(
                adRepository,
                adAttributesRepository,
                detailPageFetcher,
                adDetailExtractor,
                properties,
                clock
        );

        EnrichmentJobResult result = service.enrichPendingAds();

        assertThat(result.adsSelected()).isEqualTo(1);
        assertThat(result.adsAttempted()).isEqualTo(1);
        assertThat(result.adsEnriched()).isEqualTo(1);
        assertThat(result.adsFailed()).isZero();
        assertThat(result.adsSkipped()).isZero();
        assertThat(result.retryableFailures()).isZero();
        assertThat(result.failureMessages()).isEmpty();
        verify(adAttributesRepository).upsert(anyLong(), any(), any(), any(), any(), any());
        verify(adRepository).markEnrichmentSucceeded(anyLong(), any());
        verify(adRepository, never()).markEnrichmentFailed(anyLong(), any(), anyBoolean());
    }

    @Test
    void shouldRecordRetryableFailuresAndContinue() {
        EnrichmentProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);
        when(adRepository.findCandidatesForEnrichment(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(ad(1L), ad(2L)));
        when(adRepository.markEnrichmentStarted(anyLong(), any())).thenReturn(1);
        when(detailPageFetcher.fetch("https://999.md/ro/1"))
                .thenThrow(new EnrichmentException("navigation timeout", true));
        when(detailPageFetcher.fetch("https://999.md/ro/2"))
                .thenReturn(new PageFetchResult("https://999.md/ro/2", "<html></html>", 100L));
        when(adDetailExtractor.extract(any(), any(), anyLong()))
                .thenReturn(new ExtractedAdDetails(
                        2L,
                        "https://999.md/ro/2",
                        "Mini PC",
                        null,
                        "embedded_page_state",
                        List.of(),
                        java.util.Map.of(),
                        java.util.Map.of(),
                        java.util.Map.of(),
                        100L
                ));

        DetailEnrichmentService service = new DetailEnrichmentService(
                adRepository,
                adAttributesRepository,
                detailPageFetcher,
                adDetailExtractor,
                properties,
                clock
        );

        EnrichmentJobResult result = service.enrichPendingAds();

        assertThat(result.adsSelected()).isEqualTo(2);
        assertThat(result.adsAttempted()).isEqualTo(2);
        assertThat(result.adsEnriched()).isEqualTo(1);
        assertThat(result.adsFailed()).isEqualTo(1);
        assertThat(result.retryableFailures()).isEqualTo(1);
        assertThat(result.failureMessages()).hasSize(1);
        assertThat(result.failureMessages().getFirst()).contains("navigation timeout");
        verify(adRepository).markEnrichmentFailed(org.mockito.ArgumentMatchers.eq(1L), any(), org.mockito.ArgumentMatchers.eq(true));
        verify(adRepository).markEnrichmentSucceeded(org.mockito.ArgumentMatchers.eq(2L), any());
    }

    @Test
    void shouldSkipCandidateWhenAnotherWorkerAlreadyMarkedItInProgress() {
        EnrichmentProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);
        when(adRepository.findCandidatesForEnrichment(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(ad(42L)));
        when(adRepository.markEnrichmentStarted(anyLong(), any())).thenReturn(0);

        DetailEnrichmentService service = new DetailEnrichmentService(
                adRepository,
                adAttributesRepository,
                detailPageFetcher,
                adDetailExtractor,
                properties,
                clock
        );

        EnrichmentJobResult result = service.enrichPendingAds();

        assertThat(result.adsSelected()).isEqualTo(1);
        assertThat(result.adsAttempted()).isZero();
        assertThat(result.adsSkipped()).isEqualTo(1);
        verify(detailPageFetcher, never()).fetch(any());
        verify(adAttributesRepository, never()).upsert(anyLong(), any(), any(), any(), any(), any());
        verify(adRepository, never()).markEnrichmentSucceeded(anyLong(), any());
    }

    private EnrichmentProperties properties() {
        EnrichmentProperties properties = new EnrichmentProperties();
        properties.setBatchSize(10);
        properties.setDelayMs(0);
        properties.setRetryLimit(3);
        properties.setRefreshDays(30);
        properties.setBaseUrl("https://999.md");
        properties.setLanguage("ro");
        return properties;
    }

    private AdsRecord ad(long id) {
        AdsRecord record = new AdsRecord();
        record.setId(id);
        record.setStatus("active");
        return record;
    }
}
