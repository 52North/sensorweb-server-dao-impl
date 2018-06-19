
package org.n52.series.springdata.query;

import static org.n52.series.db.dao.QueryUtils.parseToId;
import static org.n52.series.db.dao.QueryUtils.parseToIds;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QOfferingEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.QueryUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPQLQuery;

public class OfferingQuerySpecifications extends ParameterQuerySpecifications {

    public static OfferingQuerySpecifications of(DbQuery dbQuery) {
        return new OfferingQuerySpecifications(dbQuery);
    }

    private OfferingQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches offerings included in a result of a given subquery, i.e.
     *
     * <pre>
     *   where id in (select fk_offering_id from dataset where &lt;subquery&gt;)
     * </pre>
     *
     * @param subquery
     *        the query
     * @return a boolean expression
     */
    public BooleanExpression selectFrom(JPQLQuery<DatasetEntity> subquery) {
        QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        QOfferingEntity offeringentity = QOfferingEntity.offeringEntity;
        return offeringentity.id.in(subquery.select(datasetentity.offering.id));
    }

    public BooleanExpression matchesPublicOffering(String id) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(dbQuery);
        BooleanExpression datasetPredicate = dsFilterSpec.matchOfferings(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
