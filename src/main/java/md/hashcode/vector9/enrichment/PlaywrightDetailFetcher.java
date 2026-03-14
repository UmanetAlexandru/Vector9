package md.hashcode.vector9.enrichment;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class PlaywrightDetailFetcher implements DetailPageFetcher, DisposableBean {

    private static final String EMBEDDED_AD_VIEW_MARKER = "\"adView\":";
    private static final long CONTENT_POLL_INTERVAL_MS = 500L;
    private static final long MIN_USEFUL_HTML_LENGTH = 2_000L;

    private final EnrichmentProperties properties;

    private final Object lifecycleMonitor = new Object();
    private Playwright playwright;
    private Browser browser;

    public PlaywrightDetailFetcher(EnrichmentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PageFetchResult fetch(String url) {
        long startedAt = System.nanoTime();
        BrowserContext context = null;
        Page page = null;

        try {
            context = browser().newContext();
            page = context.newPage();
            page.setDefaultTimeout(properties.getPageTimeoutMs());
            page.setDefaultNavigationTimeout(properties.getNavigationTimeoutMs());
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            String html = waitForUsefulPageContent(page);
            return new PageFetchResult(url, html, elapsedMillis(startedAt));
        } catch (TimeoutError exception) {
            throw new EnrichmentException("Timed out while fetching ad detail page %s".formatted(url), exception, true);
        } catch (RuntimeException exception) {
            throw new EnrichmentException("Failed to fetch ad detail page %s".formatted(url), exception, true);
        } finally {
            if (page != null) {
                page.close();
            }
            if (context != null) {
                context.close();
            }
        }
    }

    private String waitForUsefulPageContent(Page page) {
        long deadline = System.nanoTime() + properties.getPageTimeoutMs() * 1_000_000L;
        String lastHtml = "";

        while (System.nanoTime() < deadline) {
            lastHtml = page.content();
            if (containsUsefulContent(lastHtml)) {
                return lastHtml;
            }
            page.waitForTimeout(CONTENT_POLL_INTERVAL_MS);
        }

        if (containsUsefulFallback(lastHtml)) {
            return lastHtml;
        }

        throw new TimeoutError("Timed out waiting for useful ad detail page content");
    }

    private boolean containsUsefulContent(String html) {
        return html != null && html.contains(EMBEDDED_AD_VIEW_MARKER);
    }

    private boolean containsUsefulFallback(String html) {
        return html != null && html.length() >= MIN_USEFUL_HTML_LENGTH;
    }

    private Browser browser() {
        synchronized (lifecycleMonitor) {
            if (browser == null) {
                playwright = Playwright.create();
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(properties.isHeadless()));
            }
            return browser;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    @Override
    public void destroy() {
        synchronized (lifecycleMonitor) {
            if (browser != null) {
                browser.close();
                browser = null;
            }
            if (playwright != null) {
                playwright.close();
                playwright = null;
            }
        }
    }
}