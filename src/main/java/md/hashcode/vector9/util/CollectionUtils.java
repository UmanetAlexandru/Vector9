package md.hashcode.vector9.util;

import java.util.ArrayList;
import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return partitions;
    }
}
