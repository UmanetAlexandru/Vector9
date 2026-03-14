package md.hashcode.vector9.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import md.hashcode.vector9.repository.JobExecutionStateRepository;
import md.hashcode.vector9.service.JobExecutionSnapshot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobFreshnessHealthIndicatorTest {

    @Test
    void shouldReportUpWhenJobHasNotRunYet() {
        JobExecutionStateRepository repository = mock(JobExecutionStateRepository.class);
        when(repository.findByJobName("incremental-crawler")).thenReturn(Optional.empty());

        JobFreshnessHealthIndicator indicator = new JobFreshnessHealthIndicator(
                repository,
                Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC),
                "incremental-crawler",
                "Incremental crawler",
                30
        );

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails().get("freshness")).isEqualTo("not_yet_run");
    }

    @Test
    void shouldReportDownWhenJobIsStale() {
        JobExecutionStateRepository repository = mock(JobExecutionStateRepository.class);
        when(repository.findByJobName("incremental-crawler")).thenReturn(Optional.of(
                new JobExecutionSnapshot(
                        "incremental-crawler",
                        LocalDateTime.of(2026, 3, 14, 10, 0),
                        null,
                        2000L,
                        null,
                        LocalDateTime.of(2026, 3, 14, 10, 0)
                )
        ));

        JobFreshnessHealthIndicator indicator = new JobFreshnessHealthIndicator(
                repository,
                Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC),
                "incremental-crawler",
                "Incremental crawler",
                30
        );

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails().get("freshness")).isEqualTo("stale");
    }
}
