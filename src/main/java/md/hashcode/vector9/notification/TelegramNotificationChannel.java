package md.hashcode.vector9.notification;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TelegramNotificationChannel implements NotificationChannel {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final DownstreamProperties downstreamProperties;

    public TelegramNotificationChannel(OkHttpClient okHttpClient,
                                       ObjectMapper objectMapper,
                                       DownstreamProperties downstreamProperties) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.downstreamProperties = downstreamProperties;
    }

    @Override
    public void send(NotificationMessage message) {
        if (!downstreamProperties.hasTelegramConfiguration()) {
            throw new IllegalStateException("Telegram bot token and chat id must be configured before sending notifications");
        }

        Request request = new Request.Builder()
                .url(buildSendMessageUrl())
                .post(RequestBody.create(serializePayload(message), JSON))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Telegram sendMessage failed with HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string() : "";
            JsonNode payload = objectMapper.readTree(body);
            if (!payload.path("ok").asBoolean(false)) {
                throw new IllegalStateException("Telegram sendMessage returned ok=false");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Telegram sendMessage failed: " + exception.getMessage(), exception);
        }
    }

    private String buildSendMessageUrl() {
        String baseUrl = downstreamProperties.getTelegramBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/bot" + downstreamProperties.getTelegramBotToken() + "/sendMessage";
    }

    private String serializePayload(NotificationMessage message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "chat_id", downstreamProperties.getTelegramChatId(),
                    "text", message.text(),
                    "disable_web_page_preview", true
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize Telegram payload", exception);
        }
    }
}
