package org.n52.series.springdata.query;

import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;

public abstract class QuerySpecifications {

    protected final DbQuery dbQuery;

    protected QuerySpecifications(final DbQuery dbQuery) {
        this.dbQuery = dbQuery == null
            ? new DefaultDbQueryFactory().createDefault()
            : dbQuery;
    }
}
