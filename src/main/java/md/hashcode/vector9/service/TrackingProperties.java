package md.hashcode.vector9.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vector9.tracking")
public class TrackingProperties {

    private int deletionThresholdDays = 7;
    private int viewBatchSize = 50;
    private boolean writeViewHistory = true;
    private String language = "ro";
    private String sourceHeader = "desktop";

    public int getDeletionThresholdDays() {
        return deletionThresholdDays;
    }

    public void setDeletionThresholdDays(int deletionThresholdDays) {
        this.deletionThresholdDays = deletionThresholdDays;
    }

    public int getViewBatchSize() {
        return viewBatchSize;
    }

    public void setViewBatchSize(int viewBatchSize) {
        this.viewBatchSize = viewBatchSize;
    }

    public boolean isWriteViewHistory() {
        return writeViewHistory;
    }

    public void setWriteViewHistory(boolean writeViewHistory) {
        this.writeViewHistory = writeViewHistory;
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
}
