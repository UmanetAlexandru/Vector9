package md.hashcode.vector9.model;

import java.util.UUID;

public record OwnerUpsertCommand(
        UUID id,
        String login,
        String avatar,
        String createdDate,
        String businessPlan,
        String businessId,
        Boolean verified,
        String verificationDate
) {
}