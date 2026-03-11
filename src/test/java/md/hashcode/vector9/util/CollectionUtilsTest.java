package md.hashcode.vector9.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionUtilsTest {

    @Test
    void shouldPartitionListEvenly() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(3);
        assertThat(partitions.get(0)).containsExactly(1, 2, 3);
        assertThat(partitions.get(1)).containsExactly(4, 5, 6);
        assertThat(partitions.get(2)).containsExactly(7, 8, 9);
    }

    @Test
    void shouldPartitionListUnevenly() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(3);
        assertThat(partitions.get(0)).containsExactly(1, 2, 3);
        assertThat(partitions.get(1)).containsExactly(4, 5, 6);
        assertThat(partitions.get(2)).containsExactly(7, 8);
    }

    @Test
    void shouldHandleEmptyList() {
        List<Integer> list = List.of();
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).isEmpty();
    }

    @Test
    void shouldHandleSingleElement() {
        List<Integer> list = List.of(1);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(1);
        assertThat(partitions.get(0)).containsExactly(1);
    }

    @Test
    void shouldThrowExceptionForInvalidBatchSize() {
        List<Integer> list = List.of(1, 2, 3);

        assertThatThrownBy(() -> CollectionUtils.partition(list, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be positive");

        assertThatThrownBy(() -> CollectionUtils.partition(list, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be positive");
    }
}
