
package org.n52.series.springdata.query;

import static org.n52.series.db.dao.QueryUtils.parseToIds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QGeometryEntity;
import org.n52.series.db.beans.QProcedureEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.vividsolutions.jts.geom.Geometry;

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
     * @param filter
     *        a filter each selected dataset have to match
     * @return a subquery selection only public datasets.
     */
    public JPQLQuery<DatasetEntity> toSubquery(BooleanExpression filter) {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return JPAExpressions.selectFrom(dataset)
                             .where(filter);
    }

    /**
     * Aggregates following filters in an {@literal AND} expression:
     * <ul>
     * <li>{@link #isPublic()} (also an aggregate filter)</li>
     * <li>{@link #matchFeatures()}</li>
     * <li>{@link #matchOfferings()}</li>
     * <li>{@link #matchPhenomena()}</li>
     * <li>{@link #matchProcedures()}</li>
     * <li>{@link #matchValueTypes()}</li>
     * <li>{@link #matchesSpatially()}</li>
     * </ul>
     * 
     * @return a boolean expression matching all filter criteria
     */
    public BooleanExpression matchFilters() {
        return isPublic().and(matchFeatures())
                         .and(matchOfferings())
                         .and(matchPhenomena())
                         .and(matchProcedures())
                         .and(matchValueTypes())
                         .and(matchesSpatially());
    }

    /**
     * Aggregates following filters in an {@literal AND} expression:
     *
     * <ul>
     * <li>{@link #hasFeature()}</li>
     * <li>{@link #isPublished()}</li>
     * <li>{@link #isEnabled()}</li>
     * <li>not {@link #isDeleted()}</li>
     * </ul>
     *
     * @return a boolean expression
     */
    public BooleanExpression isPublic() {
        return hasFeature().and(isDeleted().not())
                           .and(isEnabled())
                           .and(isPublished());
    }

    private BooleanExpression hasFeature() {
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.feature.isNotNull();
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
     * Matches datasets having offerings with given ids.
     *
     * @return a boolean expression
     * @see #matchOfferings(Collection)
     */
    public BooleanExpression matchOfferings() {
        IoParameters parameters = dbQuery.getParameters();
        return matchOfferings(parameters.getOfferings());
    }

    /**
     * Matches datasets having offerings with given ids.
     * 
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchOfferings(Collection)
     */
    public BooleanExpression matchOfferings(String... ids) {
        return ids != null
            ? matchOfferings(Arrays.asList(ids))
            : matchOfferings(Collections.emptyList());
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
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchOfferings(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
            ? dataset.offering.identifier.in(ids)
            : dataset.offering.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having features with given ids.
     * 
     * @return a boolean expression
     * @see #matchFeatures(Collection)
     */
    public BooleanExpression matchFeatures() {
        IoParameters parameters = dbQuery.getParameters();
        return matchFeatures(parameters.getFeatures());
    }

    /**
     * Matches datasets having features with given ids.
     * 
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchFeatures(Collection)
     */
    public BooleanExpression matchFeatures(String... ids) {
        return ids != null
            ? matchFeatures(Arrays.asList(ids))
            : matchFeatures(Collections.emptyList());
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
     * 
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchFeatures(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
            ? dataset.feature.identifier.in(ids)
            : dataset.feature.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having procedures with given ids.
     *
     * @return a boolean expression
     * @see #matchProcedures(Collection)
     */
    public BooleanExpression matchProcedures() {
        IoParameters parameters = dbQuery.getParameters();
        return matchProcedures(parameters.getProcedures());
    }

    /**
     * Matches datasets having procedures with given ids.
     * 
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchProcedures(Collection)
     */
    public BooleanExpression matchProcedures(String... ids) {
        return ids != null
            ? matchProcedures(Arrays.asList(ids))
            : matchProcedures(Collections.emptyList());
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
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchProcedures(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
            ? dataset.procedure.identifier.in(ids)
            : dataset.procedure.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets having phenomena with given ids.
     *
     * @return a boolean expression
     * @see #matchPhenomena(Collection)
     */
    public BooleanExpression matchPhenomena() {
        IoParameters parameters = dbQuery.getParameters();
        return matchPhenomena(parameters.getPhenomena());
    }

    /**
     * Matches datasets having phenomena with given ids.
     * 
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchPhenomena(Collection)
     */
    public BooleanExpression matchPhenomena(String... ids) {
        return ids != null
            ? matchPhenomena(Arrays.asList(ids))
            : matchPhenomena(Collections.emptyList());
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
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchPhenomena(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dbQuery.isMatchDomainIds()
            ? dataset.phenomenon.identifier.in(ids)
            : dataset.phenomenon.id.in(parseToIds(ids));
    }

    /**
     * Matches datasets matching given value types.
     *
     * @return a boolean expression
     * @see #matchValueTypes(Collection)
     */
    public BooleanExpression matchValueTypes() {
        IoParameters parameters = dbQuery.getParameters();
        return matchValueTypes(parameters.getValueTypes());
    }

    /**
     * Matches datasets matching given value types.
     * 
     * @param valueTypes
     *        the value types to match
     * @return a boolean expression
     * @see #matchValueTypes(Collection)
     */
    public BooleanExpression matchValueTypes(String... valueTypes) {
        return valueTypes != null
            ? matchValueTypes(Arrays.asList(valueTypes))
            : matchValueTypes(Collections.emptyList());
    }

    /**
     * Matches datasets matching given value types. For example:
     *
     * <pre>
     *  where valueType in (&lt;valueTypes&gt;)
     * </pre>
     * 
     * @param valueTypes
     *        the value types to match
     * @return a boolean expression or {@literal null} when given value types are {@literal null} or empty
     */
    public BooleanExpression matchValueTypes(Collection<String> valueTypes) {
        if (valueTypes == null || valueTypes.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.valueType.in(valueTypes);
    }

    /**
     * Matches datasets which have a feature laying within the given bbox using an intersects query.
     * 
     * @return a boolean expression or {@literal null} when given spatial filter is {@literal null} or empty
     */
    public BooleanExpression matchesSpatially() {
        Geometry geometry = dbQuery.getSpatialFilter();
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        QGeometryEntity geometryEntity = dataset.feature.geometryEntity;
        return geometryEntity.geometry.intersects(geometry);

    }

}
