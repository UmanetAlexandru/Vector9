package md.hashcode.vector9.notification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Repository
public class DownstreamEventRepository {

    private static final Table<?> DOWNSTREAM_EVENTS = table(name("downstream_events"));
    private static final Field<Long> ID = field(name("downstream_events", "id"), Long.class);
    private static final Field<String> EVENT_KEY = field(name("downstream_events", "event_key"), String.class);
    private static final Field<String> EVENT_TYPE = field(name("downstream_events", "event_type"), String.class);
    private static final Field<String> JOB_NAME = field(name("downstream_events", "job_name"), String.class);
    private static final Field<String> ENVIRONMENT_NAME = field(name("downstream_events", "environment_name"), String.class);
    private static final Field<LocalDateTime> EVENT_AT = field(name("downstream_events", "event_at"), LocalDateTime.class);
    private static final Field<String> MESSAGE_TEXT = field(name("downstream_events", "message_text"), String.class);
    private static final Field<String> STATUS = field(name("downstream_events", "status"), String.class);
    private static final Field<Integer> ATTEMPT_COUNT = field(name("downstream_events", "attempt_count"), Integer.class);
    private static final Field<LocalDateTime> LAST_ATTEMPT_AT = field(name("downstream_events", "last_attempt_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> DELIVERED_AT = field(name("downstream_events", "delivered_at"), LocalDateTime.class);
    private static final Field<String> LAST_ERROR = field(name("downstream_events", "last_error"), String.class);
    private static final Field<LocalDateTime> CREATED_AT = field(name("downstream_events", "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> UPDATED_AT = field(name("downstream_events", "updated_at"), LocalDateTime.class);

    private final DSLContext dslContext;

    public DownstreamEventRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public boolean createPendingEvent(String eventKey,
                                      DownstreamEventType eventType,
                                      String jobName,
                                      String environmentName,
                                      LocalDateTime eventAt,
                                      String messageText) {
        return dslContext.insertInto(DOWNSTREAM_EVENTS)
                .set(EVENT_KEY, eventKey)
                .set(EVENT_TYPE, eventType.name())
                .set(JOB_NAME, jobName)
                .set(ENVIRONMENT_NAME, environmentName)
                .set(EVENT_AT, eventAt)
                .set(MESSAGE_TEXT, messageText)
                .set(STATUS, DownstreamDeliveryStatus.PENDING.name())
                .set(ATTEMPT_COUNT, 0)
                .set(CREATED_AT, eventAt)
                .set(UPDATED_AT, eventAt)
                .onConflict(EVENT_KEY)
                .doNothing()
                .execute() > 0;
    }

    public java.util.List<DownstreamEvent> findDeliverableEvents(Collection<DownstreamEventType> eventTypes,
                                                                 int retryLimit,
                                                                 int limit) {
        Condition condition = STATUS.in(DownstreamDeliveryStatus.PENDING.name(), DownstreamDeliveryStatus.FAILED.name())
                .and(ATTEMPT_COUNT.lt(retryLimit));

        if (eventTypes != null && !eventTypes.isEmpty()) {
            condition = condition.and(EVENT_TYPE.in(eventTypes.stream().map(Enum::name).toList()));
        }

        return dslContext.select(
                        ID,
                        EVENT_KEY,
                        EVENT_TYPE,
                        JOB_NAME,
                        ENVIRONMENT_NAME,
                        EVENT_AT,
                        MESSAGE_TEXT,
                        STATUS,
                        ATTEMPT_COUNT,
                        LAST_ATTEMPT_AT,
                        DELIVERED_AT,
                        LAST_ERROR,
                        CREATED_AT
                )
                .from(DOWNSTREAM_EVENTS)
                .where(condition)
                .orderBy(EVENT_AT.asc(), ID.asc())
                .limit(limit)
                .fetch(record -> new DownstreamEvent(
                        record.get(ID),
                        record.get(EVENT_KEY),
                        DownstreamEventType.valueOf(record.get(EVENT_TYPE)),
                        record.get(JOB_NAME),
                        record.get(ENVIRONMENT_NAME),
                        record.get(EVENT_AT),
                        record.get(MESSAGE_TEXT),
                        DownstreamDeliveryStatus.valueOf(record.get(STATUS)),
                        record.get(ATTEMPT_COUNT) != null ? record.get(ATTEMPT_COUNT) : 0,
                        record.get(LAST_ATTEMPT_AT),
                        record.get(DELIVERED_AT),
                        record.get(LAST_ERROR),
                        record.get(CREATED_AT)
                ));
    }

    public void markSent(long id, LocalDateTime attemptedAt) {
        dslContext.update(DOWNSTREAM_EVENTS)
                .set(STATUS, DownstreamDeliveryStatus.SENT.name())
                .set(ATTEMPT_COUNT, ATTEMPT_COUNT.plus(1))
                .set(LAST_ATTEMPT_AT, attemptedAt)
                .set(DELIVERED_AT, attemptedAt)
                .set(LAST_ERROR, (String) null)
                .set(UPDATED_AT, attemptedAt)
                .where(ID.eq(id))
                .execute();
    }

    public void markFailed(long id, LocalDateTime attemptedAt, String errorMessage) {
        dslContext.update(DOWNSTREAM_EVENTS)
                .set(STATUS, DownstreamDeliveryStatus.FAILED.name())
                .set(ATTEMPT_COUNT, ATTEMPT_COUNT.plus(1))
                .set(LAST_ATTEMPT_AT, attemptedAt)
                .set(LAST_ERROR, errorMessage)
                .set(UPDATED_AT, attemptedAt)
                .where(ID.eq(id))
                .execute();
    }

    public Map<DownstreamEventType, Long> countEventsCreatedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        Map<DownstreamEventType, Long> counts = new EnumMap<>(DownstreamEventType.class);
        for (DownstreamEventType eventType : DownstreamEventType.values()) {
            counts.put(eventType, 0L);
        }

        dslContext.select(EVENT_TYPE, count())
                .from(DOWNSTREAM_EVENTS)
                .where(CREATED_AT.ge(windowStart))
                .and(CREATED_AT.lt(windowEnd))
                .groupBy(EVENT_TYPE)
                .fetch()
                .forEach(record -> counts.put(
                        DownstreamEventType.valueOf(record.get(EVENT_TYPE)),
                        record.get(1, Long.class)
                ));

        return counts;
    }
}
