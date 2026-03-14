package md.hashcode.vector9.client;

import md.hashcode.vector9.crawler.IncrementalCrawler;
import md.hashcode.vector9.crawler.SubcategoryCrawlResult;
import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Manual live crawl test. Run explicitly when network access and a writable dev database are available.")
@SpringBootTest
class ManualCrawlerMiniPcLiveTest {

    @Autowired
    private IncrementalCrawler incrementalCrawler;

    @Test
    void shouldRunOneMiniPcIncrementalCrawlAndPrintSummary() {
        SubcategoriesRecord subcategory = new SubcategoriesRecord();
        subcategory.setId(7661L);
        subcategory.setName("mini-pc");
        subcategory.setEnabled(true);
        subcategory.setIncludeCarsFeatures(false);

        SubcategoryCrawlResult result = incrementalCrawler.crawlSubcategory(subcategory);

        System.out.println(result);
    }
}
