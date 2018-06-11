
package org.n52.series.springdata.query;

import org.n52.series.db.beans.QDatasetEntity;

import com.querydsl.core.types.dsl.BooleanExpression;

public class DatasetQuerySpecifications {

    /**
     * Matches datasets where
     *
     * <pre>
     *  where published and not deleted and not disabled
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression isPublic() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.published.isTrue()
                                .and(dataset.deleted.isFalse())
                                .and(dataset.disabled.isFalse());
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where deleted
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression isDeleted() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.deleted.isTrue();
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where offering.id in (&lt;offeringIds&gt;)
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression hasOfferings(Long... offeringIds) {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.offering.id.in(offeringIds);
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where offering.id in (&lt;featureIds&gt;)
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression hasFeatures(Long... featureIds) {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.feature.id.in(featureIds);
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where offering.id in (&lt;procedureIds&gt;)
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression hasProcedures(Long... procedureIds) {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.procedure.id.in(procedureIds);
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where offering.id in (&lt;phenomenonIds&gt;)
     * </pre>
     *
     * @return a boolean expression
     */
    public static BooleanExpression hasPhenomena(Long... phenomenonIds) {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.phenomenon.id.in(phenomenonIds);
    }

}
