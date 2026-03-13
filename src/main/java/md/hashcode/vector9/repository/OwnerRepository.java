package md.hashcode.vector9.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import md.hashcode.vector9.jooq.tables.records.OwnersRecord;
import md.hashcode.vector9.model.OwnerUpsertCommand;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.OWNERS;

@Repository
public class OwnerRepository {

    private final DSLContext dslContext;

    public OwnerRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Optional<OwnersRecord> findById(UUID id) {
        return dslContext.selectFrom(OWNERS)
                .where(OWNERS.ID.eq(id))
                .fetchOptionalInto(OwnersRecord.class);
    }

    public OwnersRecord upsert(OwnerUpsertCommand command, LocalDateTime now) {
        return dslContext.insertInto(OWNERS)
                .set(OWNERS.ID, command.id())
                .set(OWNERS.LOGIN, command.login())
                .set(OWNERS.AVATAR, command.avatar())
                .set(OWNERS.CREATED_DATE, command.createdDate())
                .set(OWNERS.BUSINESS_PLAN, command.businessPlan())
                .set(OWNERS.BUSINESS_ID, command.businessId())
                .set(OWNERS.IS_VERIFIED, Boolean.TRUE.equals(command.verified()))
                .set(OWNERS.VERIFICATION_DATE, command.verificationDate())
                .set(OWNERS.FIRST_SEEN_AT, now)
                .set(OWNERS.LAST_UPDATED_AT, now)
                .onConflict(OWNERS.ID)
                .doUpdate()
                .set(OWNERS.LOGIN, command.login())
                .set(OWNERS.AVATAR, command.avatar())
                .set(OWNERS.CREATED_DATE, command.createdDate())
                .set(OWNERS.BUSINESS_PLAN, command.businessPlan())
                .set(OWNERS.BUSINESS_ID, command.businessId())
                .set(OWNERS.IS_VERIFIED, Boolean.TRUE.equals(command.verified()))
                .set(OWNERS.VERIFICATION_DATE, command.verificationDate())
                .set(OWNERS.LAST_UPDATED_AT, now)
                .returning()
                .fetchOne();
    }
}