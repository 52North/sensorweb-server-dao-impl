
package org.n52.series.db.query;

import static org.n52.io.request.Parameters.FILTER_PLATFORM_TYPES;
import static org.n52.series.db.old.dao.QueryUtils.parseToIds;

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
import org.n52.series.db.beans.dataset.NotInitializedDataset;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.vividsolutions.jts.geom.Geometry;

public class DatasetQuerySpecifications extends QuerySpecifications {

    public static DatasetQuerySpecifications of(final DbQuery dbQuery) {
        return new DatasetQuerySpecifications(dbQuery);
    }

    private DatasetQuerySpecifications(final DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * @param filter
     *        a filter each selected dataset have to match
     * @return a subquery selection only public datasets.
     */
    public JPQLQuery<DatasetEntity> toSubquery(final BooleanExpression filter) {
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
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
     * <li>{@link #matchPlatformTypes()}</li>
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
                         .and(matchPlatformTypes())
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
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
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
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
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
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
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
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return dataset.deleted.isTrue();
    }

    /**
     * Matches datasets having offerings with given ids.
     *
     * @return a boolean expression
     * @see #matchOfferings(Collection)
     */
    public BooleanExpression matchOfferings() {
        final IoParameters parameters = query.getParameters();
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
    public BooleanExpression matchOfferings(final String... ids) {
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
    public BooleanExpression matchOfferings(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return query.isMatchDomainIds()
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
        final IoParameters parameters = query.getParameters();
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
    public BooleanExpression matchFeatures(final String... ids) {
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
    public BooleanExpression matchFeatures(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return query.isMatchDomainIds()
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
        final IoParameters parameters = query.getParameters();
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
    public BooleanExpression matchProcedures(final String... ids) {
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
    public BooleanExpression matchProcedures(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return query.isMatchDomainIds()
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
        final IoParameters parameters = query.getParameters();
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
    public BooleanExpression matchPhenomena(final String... ids) {
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
    public BooleanExpression matchPhenomena(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        return query.isMatchDomainIds()
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
        final IoParameters parameters = query.getParameters();
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
    public BooleanExpression matchValueTypes(final String... valueTypes) {
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
    public BooleanExpression matchValueTypes(final Collection<String> valueTypes) {
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        if ((valueTypes == null) || valueTypes.isEmpty()) {
            return isInitializedDataset(dataset);
        }
        return isInitializedDataset(dataset).and(dataset.valueType.in(valueTypes));
    }

    private BooleanExpression isInitializedDataset(final QDatasetEntity dataset) {
        return dataset.valueType.notEqualsIgnoreCase(NotInitializedDataset.DATASET_TYPE);
    }

    /**
     * Matches datasets matching given platform types.
     *
     * @return a boolean expression
     * @see #matchPlatformTypes(Collection)
     */
    public BooleanExpression matchPlatformTypes() {
        final IoParameters parameters = query.getParameters();
        final Set<String> platformTypes = parameters.getPlatformTypes();
        return matchPlatformTypes(platformTypes);
    }

    /**
     * Matches datasets matching given platform types.
     *
     * @param platformTypes
     *        the platform types to match
     * @return a boolean expression
     * @see #matchPlatformTypes(Collection)
     */
    public BooleanExpression matchPlatformTypes(final String... platformTypes) {
        return platformTypes != null
            ? matchPlatformTypes(Arrays.asList(platformTypes))
            : null;
    }

    /**
     * Matches datasets matching given value types. For example:
     *
     * <pre>
     *  where is_mobile &lt;false&gt; and is_insitu &lt;true&gt;
     * </pre>
     *
     * @param platformTypes
     *        the platform types to match
     * @return a boolean expression or {@literal null} when given value types are {@literal null} or empty
     */
    public BooleanExpression matchPlatformTypes(final Collection<String> platformTypes) {
        if ((platformTypes == null) || platformTypes.isEmpty()) {
            return null;
        }
        final DbQuery filterQuery = query.replaceWith(FILTER_PLATFORM_TYPES, platformTypes.toArray(new String[0]));
        final FilterResolver filterResolver = filterQuery.getFilterResolver();

        final QProcedureEntity procedure = QDatasetEntity.datasetEntity.procedure;
        return isMobileOrStationary(filterResolver, procedure).and(isInsituOrRemote(filterResolver, procedure));
    }

    private BooleanExpression isInsituOrRemote(final FilterResolver filterResolver, final QProcedureEntity procedure) {
        return isRemote(procedure, filterResolver).or(isInsitu(procedure, filterResolver));
    }

    private BooleanExpression isMobileOrStationary(final FilterResolver filterResolver, final QProcedureEntity procedure) {
        return isMobile(procedure, filterResolver).or(isStationary(procedure, filterResolver));
    }

    private BooleanExpression isRemote(final QProcedureEntity procedure, final FilterResolver filterResolver) {
        return procedure.insitu.eq(filterResolver.shallIncludeRemotePlatformTypes()).not();
    }

    private BooleanExpression isStationary(final QProcedureEntity procedure, final FilterResolver filterResolver) {
        return procedure.mobile.eq(filterResolver.shallIncludeStationaryPlatformTypes()).not();
    }

    private BooleanExpression isMobile(final QProcedureEntity procedure, final FilterResolver filterResolver) {
        return procedure.mobile.eq(filterResolver.shallIncludeMobilePlatformTypes());
    }

    private BooleanExpression isInsitu(final QProcedureEntity procedure, final FilterResolver filterResolver) {
        return procedure.insitu.eq(filterResolver.shallIncludeInsituPlatformTypes());
    }

    /**
     * Matches datasets which have a feature laying within the given bbox using an intersects query. For
     * example:
     *
     * <pre>
     *   where ST_INTERSECTS(feature.geom, &lt;filter_geometry&gt;)=1
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given spatial filter is {@literal null} or empty
     */
    public BooleanExpression matchesSpatially() {
        final Geometry geometry = query.getSpatialFilter();
        if ((geometry == null) || geometry.isEmpty()) {
            return null;
        }
        final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
        final QGeometryEntity geometryEntity = dataset.feature.geometryEntity;
        return geometryEntity.geometry.intersects(geometry);
    }

}
