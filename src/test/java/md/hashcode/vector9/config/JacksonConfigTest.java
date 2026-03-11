package md.hashcode.vector9.config;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeInstantAsIsoString() throws Exception {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        String json = objectMapper.writeValueAsString(now);

        assertThat(json).contains("2026-03-10T12:00:00Z");
        assertThat(json).doesNotContain("1709827200");
    }

    @Test
    void shouldDeserializeIsoStringToInstant() throws Exception {
        String json = "\"2026-03-10T12:00:00Z\"";
        Instant instant = objectMapper.readValue(json, Instant.class);

        assertThat(instant).isEqualTo(Instant.parse("2026-03-10T12:00:00Z"));
    }
}
