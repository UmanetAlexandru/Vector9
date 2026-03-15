package md.hashcode.vector9.notification;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramNotificationChannelTest {

    @Test
    void shouldPostSendMessagePayload() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true,\"result\":{\"message_id\":1}}"));

            DownstreamProperties properties = new DownstreamProperties();
            properties.setTelegramBaseUrl(server.url("/").toString());
            properties.setTelegramBotToken("token");
            properties.setTelegramChatId("12345");

            TelegramNotificationChannel channel = new TelegramNotificationChannel(new OkHttpClient(), new ObjectMapper(), properties);
            channel.send(new NotificationMessage("[PROD] test"));

            RecordedRequest request = server.takeRequest();
            String requestBody = request.getBody().readUtf8();
            assertThat(request.getPath()).isEqualTo("/bottoken/sendMessage");
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(requestBody).contains("\"chat_id\":\"12345\"");
            assertThat(requestBody).contains("\"text\":\"[PROD] test\"");
        }
    }

    @Test
    void shouldFailWhenTelegramReturnsError() throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"ok\":false}"));

            DownstreamProperties properties = new DownstreamProperties();
            properties.setTelegramBaseUrl(server.url("/").toString());
            properties.setTelegramBotToken("token");
            properties.setTelegramChatId("12345");

            TelegramNotificationChannel channel = new TelegramNotificationChannel(new OkHttpClient(), new ObjectMapper(), properties);

            assertThatThrownBy(() -> channel.send(new NotificationMessage("test")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP 500");
        }
    }
}
