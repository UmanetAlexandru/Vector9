package md.hashcode.vector9.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import md.hashcode.vector9.model.AdImageInput;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.AD_IMAGES;

@Repository
public class AdImageRepository {

    private final DSLContext dslContext;

    public AdImageRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Set<String> findFilenamesByAdId(long adId) {
        return new LinkedHashSet<>(dslContext.select(AD_IMAGES.IMAGE_FILENAME)
                .from(AD_IMAGES)
                .where(AD_IMAGES.AD_ID.eq(adId))
                .orderBy(AD_IMAGES.POSITION.asc())
                .fetch(AD_IMAGES.IMAGE_FILENAME));
    }

    public int insertMissing(long adId, List<AdImageInput> images) {
        Set<String> existingFilenames = findFilenamesByAdId(adId);
        int nextPosition = dslContext.selectCount()
                .from(AD_IMAGES)
                .where(AD_IMAGES.AD_ID.eq(adId))
                .fetchOne(0, int.class);
        boolean hasPrimary = dslContext.fetchExists(
                dslContext.selectOne()
                        .from(AD_IMAGES)
                        .where(AD_IMAGES.AD_ID.eq(adId))
                        .and(AD_IMAGES.IS_PRIMARY.isTrue())
        );

        int inserted = 0;
        for (AdImageInput image : images) {
            if (existingFilenames.contains(image.filename())) {
                continue;
            }

            boolean isPrimary = !hasPrimary && image.primary();
            dslContext.insertInto(AD_IMAGES)
                    .set(AD_IMAGES.AD_ID, adId)
                    .set(AD_IMAGES.IMAGE_FILENAME, image.filename())
                    .set(AD_IMAGES.POSITION, nextPosition)
                    .set(AD_IMAGES.IS_PRIMARY, isPrimary)
                    .execute();

            existingFilenames.add(image.filename());
            hasPrimary = hasPrimary || isPrimary;
            nextPosition++;
            inserted++;
        }

        return inserted;
    }
}