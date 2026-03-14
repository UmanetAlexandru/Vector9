package md.hashcode.vector9.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vector9.crawler")
public class CrawlerProperties {

    private int pageSize = 50;
    private int unchangedStopThreshold = 5;
    private String locale = "ro_RO";
    private String language = "ro";
    private String sourceHeader = "desktop";
    private String inputSource = "AD_SOURCE_DESKTOP";

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getUnchangedStopThreshold() {
        return unchangedStopThreshold;
    }

    public void setUnchangedStopThreshold(int unchangedStopThreshold) {
        this.unchangedStopThreshold = unchangedStopThreshold;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceHeader() {
        return sourceHeader;
    }

    public void setSourceHeader(String sourceHeader) {
        this.sourceHeader = sourceHeader;
    }

    public String getInputSource() {
        return inputSource;
    }

    public void setInputSource(String inputSource) {
        this.inputSource = inputSource;
    }
}