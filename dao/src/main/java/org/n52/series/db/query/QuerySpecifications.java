package org.n52.series.db.query;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;

public abstract class QuerySpecifications {

    protected final DbQuery dbQuery;

    protected QuerySpecifications(final DbQuery dbQuery) {
        this.dbQuery = dbQuery == null
            ? new DefaultDbQueryFactory().createDefault()
            : dbQuery;
    }
}
