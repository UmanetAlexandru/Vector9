package md.hashcode.vector9.notification;

import java.time.Instant;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Manual Telegram smoke test. Run explicitly with VECTOR9_DOWNSTREAM_TELEGRAM_BOT_TOKEN and VECTOR9_DOWNSTREAM_TELEGRAM_CHAT_ID configured.")
@SpringBootTest
@ActiveProfiles("test")
class ManualTelegramNotificationSmokeTest {

    @Autowired
    private TelegramNotificationChannel telegramNotificationChannel;

    @Test
    void shouldSendManualSmokeMessage() {
        telegramNotificationChannel.send(new NotificationMessage(
                "[TEST] Vector9 Telegram smoke test " + Instant.now()
        ));
    }
}
