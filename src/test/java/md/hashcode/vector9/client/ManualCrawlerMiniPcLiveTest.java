package md.hashcode.vector9.client;

import md.hashcode.vector9.crawler.IncrementalCrawler;
import md.hashcode.vector9.crawler.SubcategoryCrawlResult;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Manual live crawl test. Run explicitly when network access and Testcontainers are available.")
@SpringBootTest
@ActiveProfiles("test")
class ManualCrawlerMiniPcLiveTest {

    @Autowired
    private IncrementalCrawler incrementalCrawler;
    @Autowired
    private DataSource dataSource;

    @Test
    void shouldRunOneMiniPcIncrementalCrawlAndPrintSummary() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        SubcategoriesRecord subcategory = new SubcategoriesRecord();
        subcategory.setId(7661L);
        subcategory.setName("mini-pc");
        subcategory.setEnabled(true);
        subcategory.setIncludeCarsFeatures(false);

        SubcategoryCrawlResult result = incrementalCrawler.crawlSubcategory(subcategory);

        System.out.println(result);
    }
}
