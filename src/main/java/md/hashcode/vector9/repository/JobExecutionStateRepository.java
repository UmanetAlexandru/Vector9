package md.hashcode.vector9.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import md.hashcode.vector9.service.JobExecutionSnapshot;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Repository
public class JobExecutionStateRepository {

    private static final Table<?> JOB_EXECUTION_STATE = table(name("job_execution_state"));
    private static final Field<String> JOB_NAME = field(name("job_execution_state", "job_name"), String.class);
    private static final Field<LocalDateTime> LAST_SUCCESS_AT = field(name("job_execution_state", "last_success_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> LAST_FAILURE_AT = field(name("job_execution_state", "last_failure_at"), LocalDateTime.class);
    private static final Field<Long> LAST_DURATION_MS = field(name("job_execution_state", "last_duration_ms"), Long.class);
    private static final Field<String> LAST_ERROR = field(name("job_execution_state", "last_error"), String.class);
    private static final Field<LocalDateTime> LAST_UPDATED_AT = field(name("job_execution_state", "last_updated_at"), LocalDateTime.class);

    private final DSLContext dslContext;

    public JobExecutionStateRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Optional<JobExecutionSnapshot> findByJobName(String jobName) {
        return dslContext.select(JOB_NAME, LAST_SUCCESS_AT, LAST_FAILURE_AT, LAST_DURATION_MS, LAST_ERROR, LAST_UPDATED_AT)
                .from(JOB_EXECUTION_STATE)
                .where(JOB_NAME.eq(jobName))
                .fetchOptional(record -> new JobExecutionSnapshot(
                        record.get(JOB_NAME),
                        record.get(LAST_SUCCESS_AT),
                        record.get(LAST_FAILURE_AT),
                        record.get(LAST_DURATION_MS),
                        record.get(LAST_ERROR),
                        record.get(LAST_UPDATED_AT)
                ));
    }

    public void upsertSuccess(String jobName, long durationMs, LocalDateTime recordedAt) {
        dslContext.insertInto(JOB_EXECUTION_STATE)
                .set(JOB_NAME, jobName)
                .set(LAST_SUCCESS_AT, recordedAt)
                .set(LAST_DURATION_MS, durationMs)
                .set(LAST_ERROR, (String) null)
                .set(LAST_UPDATED_AT, recordedAt)
                .onConflict(JOB_NAME)
                .doUpdate()
                .set(LAST_SUCCESS_AT, recordedAt)
                .set(LAST_DURATION_MS, durationMs)
                .set(LAST_ERROR, (String) null)
                .set(LAST_UPDATED_AT, recordedAt)
                .execute();
    }

    public void upsertFailure(String jobName, long durationMs, String errorMessage, LocalDateTime recordedAt) {
        dslContext.insertInto(JOB_EXECUTION_STATE)
                .set(JOB_NAME, jobName)
                .set(LAST_FAILURE_AT, recordedAt)
                .set(LAST_DURATION_MS, durationMs)
                .set(LAST_ERROR, errorMessage)
                .set(LAST_UPDATED_AT, recordedAt)
                .onConflict(JOB_NAME)
                .doUpdate()
                .set(LAST_FAILURE_AT, recordedAt)
                .set(LAST_DURATION_MS, durationMs)
                .set(LAST_ERROR, errorMessage)
                .set(LAST_UPDATED_AT, recordedAt)
                .execute();
    }
}
