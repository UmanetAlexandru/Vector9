package md.hashcode.vector9.model;

import java.util.List;

public record ProcessedAdCommand(
        AdUpsertCommand ad,
        OwnerUpsertCommand owner,
        List<AdImageInput> images
) {
}