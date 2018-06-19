package org.n52.series.springdata.query;

import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;

public class ParameterQuerySpecifications {

    protected final DbQuery dbQuery;
    
    protected ParameterQuerySpecifications(DbQuery dbQuery) {
        this.dbQuery = dbQuery == null
                ? new DefaultDbQueryFactory().createDefault()
                : dbQuery;
    }

}
