package md.hashcode.vector9.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import md.hashcode.vector9.model.CheckpointUpdateCommand;
import md.hashcode.vector9.model.OwnerUpsertCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class OwnerRepositoryIntegrationTest extends RepositoryIntegrationTestSupport {

    @Test
    void shouldListEnabledSeededSubcategories() {
        assertThat(subcategoryRepository.findEnabled())
                .extracting(record -> record.getId(), record -> record.getName())
                .containsExactly(tuple(7661L, "Mini PC"));
    }

    @Test
    void shouldInsertAndUpdateOwnerByUuid() {
        UUID ownerId = UUID.randomUUID();

        ownerRepository.upsert(
                new OwnerUpsertCommand(ownerId, "seller-a", "avatar-1", "2024-01-01", "basic", "biz-1", false, null),
                LocalDateTime.of(2026, 3, 13, 8, 0)
        );
        ownerRepository.upsert(
                new OwnerUpsertCommand(ownerId, "seller-a", "avatar-2", "2024-01-01", "premium", "biz-1", true, "2026-03-13"),
                LocalDateTime.of(2026, 3, 13, 8, 5)
        );

        var owner = ownerRepository.findById(ownerId).orElseThrow();
        assertThat(owner.getLogin()).isEqualTo("seller-a");
        assertThat(owner.getAvatar()).isEqualTo("avatar-2");
        assertThat(owner.getBusinessPlan()).isEqualTo("premium");
        assertThat(owner.getIsVerified()).isTrue();
        assertThat(owner.getVerificationDate()).isEqualTo("2026-03-13");
    }

    @Test
    void shouldUpsertAndClearCheckpoint() {
        crawlCheckpointRepository.upsert(
                new CheckpointUpdateCommand(7661L, 50, 1000, 50),
                LocalDateTime.of(2026, 3, 13, 8, 10)
        );
        crawlCheckpointRepository.upsert(
                new CheckpointUpdateCommand(7661L, 100, 1000, 100),
                LocalDateTime.of(2026, 3, 13, 8, 20)
        );

        var checkpoint = crawlCheckpointRepository.findBySubcategoryId(7661L).orElseThrow();
        assertThat(checkpoint.getCurrentSkip()).isEqualTo(100);
        assertThat(checkpoint.getAdsProcessed()).isEqualTo(100);

        crawlCheckpointRepository.clear(7661L);
        assertThat(crawlCheckpointRepository.findBySubcategoryId(7661L)).isEmpty();
    }
}