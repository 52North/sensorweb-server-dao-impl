package org.n52.series.springdata.query;

import org.n52.series.db.dao.DbQuery;

public class ParameterQuerySpecifications extends QuerySpecifications {

    public static ParameterQuerySpecifications of(final DbQuery dbQuery) {
        return new ParameterQuerySpecifications(dbQuery);
    }

    protected ParameterQuerySpecifications(final DbQuery dbQuery) {
        super(dbQuery);
    }

}
