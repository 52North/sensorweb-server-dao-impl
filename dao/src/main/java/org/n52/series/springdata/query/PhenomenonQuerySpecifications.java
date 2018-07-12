
package org.n52.series.springdata.query;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QPhenomenonEntity;
import org.n52.series.db.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class PhenomenonQuerySpecifications extends ParameterQuerySpecifications {

    public static PhenomenonQuerySpecifications of(DbQuery dbQuery) {
        return new PhenomenonQuerySpecifications(dbQuery);
    }

    private PhenomenonQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches phenomena included in a result of a given subquery, i.e.
     *
     * <pre>
     *   where id in (select fk_phenomenon_id from dataset where &lt;subquery&gt;)
     * </pre>
     *
     * @param subquery
     *        the query
     * @return a boolean expression
     */
    public BooleanExpression selectFrom(JPQLQuery<DatasetEntity> subquery) {
        QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        QPhenomenonEntity phenomenonentity = QPhenomenonEntity.phenomenonEntity;
        return phenomenonentity.id.in(subquery.select(datasetentity.phenomenon.id));
    }

    public BooleanExpression matchesPublicOffering(String id) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(dbQuery);
        BooleanExpression datasetPredicate = dsFilterSpec.matchPhenomena(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
