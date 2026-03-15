package md.hashcode.vector9.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vector9.downstream")
public class DownstreamProperties {

    private boolean enabled = false;
    private String environmentName = "DEV";
    private int batchSize = 20;
    private int retryLimit = 3;
    private String telegramBaseUrl = "https://api.telegram.org";
    private String telegramBotToken = "";
    private String telegramChatId = "";
    private boolean dailySummaryEnabled = true;
    private String dailySummaryCron = "0 0 8 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public String getTelegramBaseUrl() {
        return telegramBaseUrl;
    }

    public void setTelegramBaseUrl(String telegramBaseUrl) {
        this.telegramBaseUrl = telegramBaseUrl;
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isDailySummaryEnabled() {
        return dailySummaryEnabled;
    }

    public void setDailySummaryEnabled(boolean dailySummaryEnabled) {
        this.dailySummaryEnabled = dailySummaryEnabled;
    }

    public String getDailySummaryCron() {
        return dailySummaryCron;
    }

    public void setDailySummaryCron(String dailySummaryCron) {
        this.dailySummaryCron = dailySummaryCron;
    }

    public boolean hasTelegramConfiguration() {
        return telegramBotToken != null
                && !telegramBotToken.isBlank()
                && telegramChatId != null
                && !telegramChatId.isBlank();
    }
}
