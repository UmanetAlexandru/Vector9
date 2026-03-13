package md.hashcode.vector9.repository;

import java.util.List;
import java.util.Optional;

import md.hashcode.vector9.jooq.tables.records.SubcategoriesRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static md.hashcode.vector9.jooq.Tables.SUBCATEGORIES;

@Repository
public class SubcategoryRepository {

    private final DSLContext dslContext;

    public SubcategoryRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public List<SubcategoriesRecord> findEnabled() {
        return dslContext.selectFrom(SUBCATEGORIES)
                .where(SUBCATEGORIES.ENABLED.isTrue())
                .orderBy(SUBCATEGORIES.ID.asc())
                .fetchInto(SubcategoriesRecord.class);
    }

    public Optional<SubcategoriesRecord> findById(long id) {
        return dslContext.selectFrom(SUBCATEGORIES)
                .where(SUBCATEGORIES.ID.eq(id))
                .fetchOptionalInto(SubcategoriesRecord.class);
    }
}