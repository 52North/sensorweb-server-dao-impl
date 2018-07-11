
package org.n52.series.db.query;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QOfferingEntity;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class OfferingQuerySpecifications extends ParameterQuerySpecifications {

    public static OfferingQuerySpecifications of(final DbQuery dbQuery) {
        return new OfferingQuerySpecifications(dbQuery);
    }

    private OfferingQuerySpecifications(final DbQuery dbQuery) {
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
    public BooleanExpression selectFrom(final JPQLQuery<DatasetEntity> subquery) {
        final QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        final QOfferingEntity offeringentity = QOfferingEntity.offeringEntity;
        return offeringentity.id.in(subquery.select(datasetentity.offering.id));
    }

    public BooleanExpression matchesPublicOffering(final String id) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        final BooleanExpression datasetPredicate = dsFilterSpec.matchOfferings(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
