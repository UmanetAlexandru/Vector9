package md.hashcode.vector9.notification;

import java.util.List;

public record NotificationDispatchResult(
        int eventsCreated,
        int eventsAttempted,
        int eventsSent,
        int eventsFailed,
        List<String> failureMessages
) {
}
