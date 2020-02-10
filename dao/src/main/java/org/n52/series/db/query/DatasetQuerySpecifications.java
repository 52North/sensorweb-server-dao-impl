/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.db.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.old.dao.DbQuery;
import org.springframework.data.jpa.domain.Specification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class DatasetQuerySpecifications extends QuerySpecifications {

    private DatasetQuerySpecifications(final DbQuery dbQuery, EntityManager entityManager) {
        super(dbQuery, entityManager);
    }

    public static DatasetQuerySpecifications of(final DbQuery dbQuery, EntityManager entityManager) {
        return new DatasetQuerySpecifications(dbQuery, entityManager);
    }

    /**
     * @param filter
     *            a filter each selected dataset have to match
     * @return a subquery selection only public datasets.
     */
    public Specification<DatasetEntity> toSubquery(final Specification<DatasetEntity> filter) {
        return filter;
        // return (root, query, builder) -> {
        // return
        // builder.in(root.get(DescribableEntity.PROPERTY_ID)).value(filter.toPredicate(root,
        // query, builder));
        // };
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
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Specification<DatasetEntity> matchFilters() {
        return isPublic().and(matchFeatures()).and(matchCategory()).and(matchPhenomena()).and(matchProcedures())
                .and(matchOfferings()).and(matchPlatforms()).and(matchDatasetTypes()).and(matchObservationTypes())
                .and(matchValueTypes()).and(matchesSpatially());
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
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Specification<DatasetEntity> isPublic() {
        return Specification.where(hasFeature()).and(isNotDeleted()).and(isEnabled()).and(isPublished());
    }
    // public Specification<String> getIdSubqueryWithFilter(Specification
    // filter) {
    // return this.toSubquery(AbstractFeatureEntity.class,
    // AbstractFeatureEntity.PROPERTY_IDENTIFIER, filter);
    // }

    protected Specification<DatasetEntity> hasFeature() {
        return (root, query, builder) -> builder.isNotNull(root.get(DatasetEntity.PROPERTY_FEATURE));
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
    // public BooleanExpression isPublished() {
    // final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
    // return dataset.published.isTrue();
    // }
    public Specification<DatasetEntity> isPublished() {
        return (root, query, builder) -> builder.isTrue(root.get(DatasetEntity.PROPERTY_PUBLISHED));
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
    // public BooleanExpression isEnabled() {
    // final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
    // return dataset.disabled.isFalse();
    // }
    public Specification<DatasetEntity> isEnabled() {
        return (root, query, builder) -> builder.isFalse(root.get(DatasetEntity.PROPERTY_DISABLED));
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
    // public BooleanExpression isDeleted() {
    // final QDatasetEntity dataset = QDatasetEntity.datasetEntity;
    // return dataset.deleted.isTrue();
    // }
    public Specification<DatasetEntity> isDeleted() {
        return (root, query, builder) -> builder.isTrue(root.get(DatasetEntity.PROPERTY_DELETED));
    }

    public Specification<DatasetEntity> isNotDeleted() {
        return (root, query, builder) -> builder.isFalse(root.get(DatasetEntity.PROPERTY_DELETED));
    }

    /**
     * Matches datasets having offerings with given ids.
     *
     * @return a boolean expression
     * @see #matchOfferings(Collection)
     */
    public Specification<DatasetEntity> matchOfferings() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchOfferings(parameters.getOfferings());
    }

    /**
     * Matches datasets having offerings with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchOfferings(Collection)
     */
    public Specification<DatasetEntity> matchOfferings(final String... ids) {
        return ids != null ? matchOfferings(Arrays.asList(ids)) : matchOfferings(Collections.emptyList());
    }

    /**
     * Matches datasets having offerings with given ids. For example:
     *
     * <pre>
     *  where offering.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where offering.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchOfferings(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, OfferingEntity> join =
                    root.join(DatasetEntity.PROPERTY_OFFERING, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having features with given ids.
     *
     * @return a boolean expression
     * @see #matchFeatures(Collection)
     */
    public Specification<DatasetEntity> matchFeatures() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchFeatures(parameters.getFeatures());
    }

    /**
     * Matches datasets having features with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchFeatures(Collection)
     */
    public Specification<DatasetEntity> matchFeatures(final String... ids) {
        return ids != null ? matchFeatures(Arrays.asList(ids)) : matchFeatures(Collections.emptyList());
    }

    /**
     * Matches datasets having features with given ids. For example:
     *
     * <pre>
     *  where feature.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where feature.identifier in (&lt;ids&gt;)
     * </pre>
     *
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchFeatures(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, AbstractFeatureEntity> join =
                    root.join(DatasetEntity.PROPERTY_FEATURE, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having procedures with given ids.
     *
     * @return a boolean expression
     * @see #matchProcedures(Collection)
     */
    public Specification<DatasetEntity> matchProcedures() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchProcedures(parameters.getProcedures());
    }

    /**
     * Matches datasets having procedures with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchProcedures(Collection)
     */
    public Specification<DatasetEntity> matchProcedures(final String... ids) {
        return ids != null ? matchProcedures(Arrays.asList(ids)) : matchProcedures(Collections.emptyList());
    }

    /**
     * Matches datasets having procedures with given ids. For example:
     *
     * <pre>
     *  where procedure.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where procedure.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchProcedures(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, ProcedureEntity> join =
                    root.join(DatasetEntity.PROPERTY_PROCEDURE, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having phenomena with given ids.
     *
     * @return a boolean expression
     * @see #matchPhenomena(Collection)
     */
    public Specification<DatasetEntity> matchPhenomena() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchPhenomena(parameters.getPhenomena());
    }

    /**
     * Matches datasets having phenomena with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchPhenomena(Collection)
     */
    public Specification<DatasetEntity> matchPhenomena(final String... ids) {
        return ids != null ? matchPhenomena(Arrays.asList(ids)) : matchPhenomena(Collections.emptyList());
    }

    /**
     * Matches datasets having phenomena with given ids. For example:
     *
     * <pre>
     *  where phenomenon.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where phenomenon.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchPhenomena(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, PhenomenonEntity> join =
                    root.join(DatasetEntity.PROPERTY_PHENOMENON, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having category with given ids.
     *
     * @return a boolean expression
     * @see #matchCategory(Collection)
     */
    public Specification<DatasetEntity> matchCategory() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchCategory(parameters.getCategories());
    }

    /**
     * Matches datasets having category with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchCategory(Collection)
     */
    public Specification<DatasetEntity> matchCategory(final String... ids) {
        return ids != null ? matchCategory(Arrays.asList(ids)) : matchCategory(Collections.emptyList());
    }

    /**
     * Matches datasets having category with given ids. For example:
     *
     * <pre>
     *  where category.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where category.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchCategory(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, CategoryEntity> join =
                    root.join(DatasetEntity.PROPERTY_CATEGORY, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having platform with given ids.
     *
     * @return a boolean expression
     * @see #matchPlatforms(Collection)
     */
    public Specification<DatasetEntity> matchPlatforms() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchPlatforms(parameters.getPlatforms());
    }

    /**
     * Matches datasets having platform with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchPlatforms(Collection)
     */
    public Specification<DatasetEntity> matchPlatforms(final String... ids) {
        return ids != null ? matchPlatforms(Arrays.asList(ids)) : matchPlatforms(Collections.emptyList());
    }

    /**
     * Matches datasets having platform with given ids. For example:
     *
     * <pre>
     *  where platform.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where platform.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchPlatforms(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, PlatformEntity> join =
                    root.join(DatasetEntity.PROPERTY_PLATFORM, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets having service with given ids.
     *
     * @return a boolean expression
     * @see #matchServices(Collection)
     */
    public Specification<DatasetEntity> matchServices() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchServices(parameters.getServices());
    }

    /**
     * Matches datasets having service with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchServices(Collection)
     */
    public Specification<DatasetEntity> matchServices(final String... ids) {
        return ids != null ? matchServices(Arrays.asList(ids)) : matchServices(Collections.emptyList());
    }

    /**
     * Matches datasets having service with given ids. For example:
     *
     * <pre>
     *  where service.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the
     * following query path will be used:
     *
     * <pre>
     *  where service.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are
     *         {@literal null} or empty
     */
    public Specification<DatasetEntity> matchServices(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, ServiceEntity> join = root.join(DatasetEntity.PROPERTY_SERVICE, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    /**
     * Matches datasets matching given dataset types.
     *
     * @return a boolean expression
     * @see #matchDatasetTypes(Collection)
     */
    public Specification<DatasetEntity> matchDatasetTypes() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchDatasetTypes(parameters.getDatasetTypes());
    }

    /**
     * Matches datasets matching given dataset types.
     *
     * @param datasetTypes
     *            the dataset types to match
     * @return a boolean expression
     * @see #matchDatasetTypes(Collection)
     */
    public Specification<DatasetEntity> matchDatasetTypes(final String... datasetTypes) {
        return datasetTypes != null ? matchDatasetTypes(Arrays.asList(datasetTypes))
                : matchDatasetTypes(Collections.emptyList());
    }

    /**
     * Matches datasets matching given dataset types. For example:
     *
     * <pre>
     *  where datasetType in (&lt;datasetTypes&gt;)
     * </pre>
     *
     * @param datasetTypes
     *            the dataset types to match
     * @return a boolean expression or {@literal null} when given dataset types
     *         are {@literal null} or empty
     */
    public Specification<DatasetEntity> matchDatasetTypes(final Collection<String> datasetTypes) {
        return (root, query, builder) -> {
            if ((datasetTypes == null) || datasetTypes.isEmpty()) {
                return builder.notEqual(root.get(DatasetEntity.PROPERTY_DATASET_TYPE), DatasetType.not_initialized);
            }
            return root.get(DatasetEntity.PROPERTY_DATASET_TYPE).in(DatasetType.convert(datasetTypes));
        };
    }

    /**
     * Matches datasets matching given observation types.
     *
     * @return a boolean expression
     * @see #matchObservationTypes(Collection)
     */
    public Specification<DatasetEntity> matchObservationTypes() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchObservationTypes(parameters.getObservationTypes());
    }

    /**
     * Matches datasets matching given observation types.
     *
     * @param observationTypes
     *            the observation types to match
     * @return a boolean expression
     * @see #matchObservationTypes(Collection)
     */
    public Specification<DatasetEntity> matchObservationTypes(final String... observationTypes) {
        return observationTypes != null ? matchObservationTypes(Arrays.asList(observationTypes))
                : matchObservationTypes(Collections.emptyList());
    }

    /**
     * Matches datasets matching given observation types. For example:
     *
     * <pre>
     *  where observationType in (&lt;observationTypes&gt;)
     * </pre>
     *
     * @param observationTypes
     *            the observation types to match
     * @return a boolean expression or {@literal null} when given observation
     *         types are {@literal null} or empty
     */
    public Specification<DatasetEntity> matchObservationTypes(final Collection<String> observationTypes) {
        return (root, query, builder) -> {
            if ((observationTypes == null) || observationTypes.isEmpty()) {
                return builder.notEqual(root.get(DatasetEntity.PROPERTY_OBSERVATION_TYPE),
                        ObservationType.not_initialized);
            }
            return root.get(DatasetEntity.PROPERTY_OBSERVATION_TYPE).in(ObservationType.convert(observationTypes));
        };
    }

    /**
     * Matches datasets matching given value types.
     *
     * @return a boolean expression
     * @see #matchValueTypes(Collection)
     */
    public Specification<DatasetEntity> matchValueTypes() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchValueTypes(parameters.getValueTypes());
    }

    /**
     * Matches datasets matching given value types.
     *
     * @param valueTypes
     *            the value types to match
     * @return a boolean expression
     * @see #matchValueTypes(Collection)
     */
    public Specification<DatasetEntity> matchValueTypes(final String... valueTypes) {
        return valueTypes != null ? matchValueTypes(Arrays.asList(valueTypes))
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
     *            the value types to match
     * @return a boolean expression or {@literal null} when given value types
     *         are {@literal null} or empty
     */
    public Specification<DatasetEntity> matchValueTypes(final Collection<String> valueTypes) {
        return (root, query, builder) -> {
            if ((valueTypes == null) || valueTypes.isEmpty()) {
                return builder.notEqual(root.get(DatasetEntity.PROPERTY_VALUE_TYPE), ValueType.not_initialized);
            }
            return root.get(DatasetEntity.PROPERTY_VALUE_TYPE).in(ValueType.convert(valueTypes));
        };
    }

    public Specification<DatasetEntity> matchId(final String id) {
        return (root, query, builder) -> {
            if (id == null || id.isEmpty()) {
                return builder.isNull(root.get(DatasetEntity.PROPERTY_ID));
            }
            return builder.equal(root.get(DatasetEntity.PROPERTY_ID), Long.parseLong(id));
        };
    }

    /**
     * Matches datasets which have a feature laying within the given bbox using
     * an intersects query. For example:
     *
     * <pre>
     *   where ST_INTERSECTS(feature.geom, &lt;filter_geometry&gt;)=1
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given spatial filter
     *         is {@literal null} or empty
     */
    public Specification<DatasetEntity> matchesSpatially() {
        final Geometry geometry = dbQuery.getSpatialFilter();
        if ((geometry == null) || geometry.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, AbstractFeatureEntity> join =
                    root.join(DatasetEntity.PROPERTY_FEATURE, JoinType.INNER);
            return new IntersectsPredicate((CriteriaBuilderImpl) builder,
                    join.get(AbstractFeatureEntity.PROPERTY_GEOMETRY_ENTITY)
                            .get(AbstractFeatureEntity.PROPERTY_GEOMETRY),
                    new LiteralExpression<>((CriteriaBuilderImpl) builder, geometry), entityManager);
        };
    }

}
