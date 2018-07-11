package org.n52.series.db.query;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;

public abstract class QuerySpecifications {

    protected final DbQuery query;

    protected QuerySpecifications(final DbQuery query) {
        this.query = query == null
            ? new DefaultDbQueryFactory().createDefault()
            : query;
    }

    protected Date getTimespanStart() {
        DateTime start = getTimespan().getStart();
        return start.toDate();
    }

    protected Date getTimespanEnd() {
        DateTime end = getTimespan().getEnd();
        return end.toDate();
    }

    private Interval getTimespan() {
        return query.getTimespan();
    }

}
