package org.n52.series.srv;

import org.n52.io.response.CategoryOutput;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;

public class CategoryService extends AccessService<CategoryOutput> {

    public CategoryService(OutputAssembler<CategoryOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
