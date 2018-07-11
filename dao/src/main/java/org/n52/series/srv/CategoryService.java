package org.n52.series.srv;

import org.n52.io.response.CategoryOutput;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class CategoryService extends AccessService<CategoryOutput> {

    public CategoryService(OutputAssembler<CategoryOutput> assembler, DbQueryFactory queryFactory) {
        super(assembler, queryFactory);
    }

}
