package org.n52.series.db.query;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QFeatureEntity;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class FeatureQuerySpecifications extends ParameterQuerySpecifications {

	public static FeatureQuerySpecifications of(DbQuery dbQuery) {
        return new FeatureQuerySpecifications(dbQuery);
    }

    private FeatureQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches procedures included in a result of a given subquery, i.e.
     *
     * <pre>
     *   where id in (select fk_procedure_id from dataset where &lt;subquery&gt;)
     * </pre>
     *
     * @param subquery
     *        the query
     * @return a boolean expression
     */
    public BooleanExpression selectFrom(JPQLQuery<DatasetEntity> subquery) {
        QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        QFeatureEntity featureentity = QFeatureEntity.featureEntity;
        return featureentity.id.in(subquery.select(datasetentity.feature.id));
    }

    public BooleanExpression matchesPublicFeature(String id) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        BooleanExpression datasetPredicate = dsFilterSpec.matchFeatures(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
