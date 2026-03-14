package md.hashcode.vector9.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vector9.enrichment")
public class EnrichmentProperties {

    private boolean enabled = false;
    private String scheduleCron = "0 0 1 * * *";
    private int batchSize = 20;
    private long delayMs = 3000;
    private int refreshDays = 30;
    private int retryLimit = 3;
    private String language = "ro";
    private String baseUrl = "https://999.md";
    private boolean headless = true;
    private long pageTimeoutMs = 30000;
    private long navigationTimeoutMs = 30000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public int getRefreshDays() {
        return refreshDays;
    }

    public void setRefreshDays(int refreshDays) {
        this.refreshDays = refreshDays;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public long getPageTimeoutMs() {
        return pageTimeoutMs;
    }

    public void setPageTimeoutMs(long pageTimeoutMs) {
        this.pageTimeoutMs = pageTimeoutMs;
    }

    public long getNavigationTimeoutMs() {
        return navigationTimeoutMs;
    }

    public void setNavigationTimeoutMs(long navigationTimeoutMs) {
        this.navigationTimeoutMs = navigationTimeoutMs;
    }
}
