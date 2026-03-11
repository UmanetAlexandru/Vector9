package md.hashcode.vector9.config;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OkHttpConfigTest {

    @Autowired
    private OkHttpClient okHttpClient;

    @Test
    void shouldConfigureHttpClient() {
        assertThat(okHttpClient).isNotNull();
        assertThat(okHttpClient.connectTimeoutMillis()).isGreaterThan(0);
        assertThat(okHttpClient.readTimeoutMillis()).isGreaterThan(0);
    }

    @Test
    void shouldEnableRetryOnConnectionFailure() {
        assertThat(okHttpClient.retryOnConnectionFailure()).isTrue();
    }
}
