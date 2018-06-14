
package org.n52.series.springdata.query;

import static org.n52.series.db.dao.QueryUtils.parseToIds;

import java.util.Set;

import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;

public class DatasetQuerySpecifications {

    private final DbQuery dbQuery;

    public static DatasetQuerySpecifications of(DbQuery dbQuery) {
        return new DatasetQuerySpecifications(dbQuery);
    }

    private DatasetQuerySpecifications(DbQuery dbQuery) {
        this.dbQuery = dbQuery == null
            ? new DefaultDbQueryFactory().createDefault()
            : dbQuery;
    }

    /**
     * @return a subquery selection only public datasets.
     */
    public JPQLQuery<DatasetEntity> filteredDatasets() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return JPAExpressions.selectFrom(dataset)
                             .where(matchFilter());
    }

    /**
     * @return a boolean expression matching all filter criteria
     */
    public BooleanExpression matchFilter() {
        return isPublic().and(matchFeatures())
                         .and(matchOfferings())
                         .and(matchPhenomena())
                         .and(matchProcedures());
    }

    /**
     * Matches datasets where
     *
     * <pre>
     *  where feature not null and published and not deleted and not disabled
     * </pre>
     *
     * @return a boolean expression
     */
    public BooleanExpression isPublic() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.feature.isNotNull()
                              .and(isDeleted())
                              .and(isEnabled())
                              .and(isPublished());
    }
    
    /**
     * Matches datasets where
     *
     * <pre>
     *  where published
     * </pre>
     *
     * @return a boolean expression
     */
    public BooleanExpression isPublished() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.published.isTrue();
    }
    
    /**
     * Matches datasets where
     *
     * <pre>
     *  where not disabled
     * </pre>
     *
     * @return a boolean expression
     */
    public BooleanExpression isEnabled() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.disabled.isFalse();
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
    public BooleanExpression isDeleted() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.deleted.isTrue();
    }

    /**
     * Matches datasets having offerings with given ids. For example:
     *
     * <pre>
     *  where offering.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where offering.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchOfferings() {
        IoParameters parameters = dbQuery.getParameters();
        Set<String> ids = parameters.getOfferings();
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
                ? dataset.offering.identifier.in(ids)
                : dataset.offering.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having features with given ids. For example:
     *
     * <pre>
     *  where feature.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where feature.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchFeatures() {
        IoParameters parameters = dbQuery.getParameters();
        Set<String> ids = parameters.getFeatures();
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
            ? dataset.feature.identifier.in(ids)
            : dataset.feature.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having procedures with given ids. For example:
     *
     * <pre>
     *  where procedure.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where procedure.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchProcedures() {
        IoParameters parameters = dbQuery.getParameters();
        Set<String> ids = parameters.getProcedures();
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
                ? dataset.procedure.identifier.in(ids)
                : dataset.procedure.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having phenomena with given ids. For example:
     *
     * <pre>
     *  where phenomenon.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where phenomenon.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchPhenomena() {
        IoParameters parameters = dbQuery.getParameters();
        Set<String> ids = parameters.getPhenomena();
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
                ? dataset.phenomenon.identifier.in(ids)
                : dataset.phenomenon.id.in(parseToIds(ids));
    }

}