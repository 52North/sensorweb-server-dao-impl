package org.n52.series.springdata.query;

import static org.n52.series.db.dao.QueryUtils.parseToIds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.QDataEntity;
import org.n52.series.db.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;

public class DataQuerySpecifications extends QuerySpecifications {

    public static DataQuerySpecifications of(final DbQuery dbQuery) {
        return new DataQuerySpecifications(dbQuery);
    }

    private DataQuerySpecifications(final DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public BooleanExpression matchDatasets() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchDatasets(parameters.getDatasets());
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public BooleanExpression matchDatasets(final String... ids) {
        return ids != null
            ? matchDatasets(Arrays.asList(ids))
            : matchDatasets(Collections.emptyList());
    }

    /**
     * Matches data of datasets with given ids. For example:
     *
     * <pre>
     *  where dataset.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where dataset.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchDatasets(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDataEntity data = QDataEntity.dataEntity;
        return dbQuery.isMatchDomainIds()
            ? data.dataset.identifier.in(ids)
            : data.dataset.id.in(parseToIds(ids));
    }
}
