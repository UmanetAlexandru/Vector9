package md.hashcode.vector9.enrichment;

import java.time.LocalDateTime;

import javax.sql.DataSource;

import md.hashcode.vector9.model.AdUpsertCommand;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.service.DetailEnrichmentService;
import md.hashcode.vector9.service.EnrichmentJobResult;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Manual live detail enrichment smoke test. Run explicitly when network access, Playwright browsers, and Testcontainers are available.")
@SpringBootTest
@ActiveProfiles("test")
class ManualDetailEnrichmentMiniPcLiveTest {

    @Autowired
    private DetailEnrichmentService detailEnrichmentService;
    @Autowired
    private AdRepository adRepository;
    @Autowired
    private DataSource dataSource;

    @Test
    void shouldEnrichOneKnownMiniPcAdAndPrintSummary() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        LocalDateTime now = LocalDateTime.of(2026, 3, 14, 12, 0);
        adRepository.upsert(new AdUpsertCommand(
                103748724L,
                "manual-mini-pc",
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
                now
        ), null, now);

        EnrichmentJobResult result = detailEnrichmentService.enrichPendingAds();

        System.out.println(result);
    }
}
