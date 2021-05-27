/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.sensorweb.server.db.query;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.springframework.data.jpa.domain.Specification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public final class DataQuerySpecifications<E extends DatasetEntity> extends QuerySpecifications {

    private DataQuerySpecifications(final DbQuery query) {
        super(query, null);
    }

    public static <E> DataQuerySpecifications of(final DbQuery query) {
        return new DataQuerySpecifications(query);
    }

    /**
     * Aggregates following filters in an {@literal AND} expression:
     * <ul>
     * <li>{@link #matchDatasets()}</li>
     * <li>{@link #matchTimespan()}</li>
     * <li>{@link #matchParentsIsNull()}</li>
     * <li>{@link #matchesSpatially()}</li>
     * </ul>
     *
     * @return a boolean expression matching all filter criteria
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Specification<DataEntity> matchFilters() {
        return matchDatasets().and(matchTimespan()).and(matchParentsIsNull()).and(matchesSpatially());
    }

    /**
     * Aggregates following filters in an {@literal AND} expression:
     * <ul>
     * <li>{@link #matchDatasets()}</li>
     * <li>{@link #matchTimespan()}</li>
     * <li>{@link #matchParentsIsNull()}</li>
     * <li>{@link #matchesSpatially()}</li>
     * </ul>
     *
     * @return a boolean expression matching all filter criteria
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Specification<DataEntity> matchFiltersParentsNotNull() {
        return matchDatasets().and(matchTimespan()).and(matchParentsIsNotNull()).and(matchesSpatially());
    }

    /**
     * Matches the timespan.
     *
     * @return a boolean expression
     */
    private Specification<DataEntity> matchTimespan() {
        return (root, query, builder) -> {
            Date requestedStart = getTimespanStart();
            Date requestedEnd = getTimespanEnd();
            return builder.and(
                    builder.greaterThanOrEqualTo(root.get(DataEntity.PROPERTY_SAMPLING_TIME_START), requestedStart),
                    builder.lessThanOrEqualTo(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END), requestedEnd));
        };

    }

    /**
     * Matches entities so that parent is null.
     *
     * @return a boolean expression
     */
    public Specification<DataEntity> matchParentsIsNull() {
        return (root, query, builder) -> builder.isNull(root.get(DataEntity.PROPERTY_PARENT));
    }

    /**
     * Matches entities so that parent is not null.
     *
     * @return a boolean expression
     */
    public Specification<DataEntity> matchParentsIsNotNull() {
        return (root, query, builder) -> builder.isNotNull(root.get(DataEntity.PROPERTY_PARENT));
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public Specification<DataEntity> matchDatasets() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchDatasets(parameters.getDatasets());
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public Specification<DataEntity> matchDatasets(final String... ids) {
        return ids != null ? matchDatasets(Arrays.asList(ids)) : matchDatasets(Collections.emptyList());
    }

    private Specification<DataEntity> matchDatasets(Long id) {
        return id != null ? matchDatasets(Arrays.asList(Long.toString(id))) : matchDatasets(Collections.emptyList());
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
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public Specification<DataEntity> matchDatasets(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DataEntity, DatasetEntity> join = root.join(DataEntity.PROPERTY_DATASET, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

    public Optional<DataEntity> matchStart(DatasetEntity dataset, EntityManager entityManager) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DataEntity> query = builder.createQuery(DataEntity.class);
        Root<DataEntity> root = query.from(DataEntity.class);
        query.select(root).orderBy(builder.desc(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END))).where(
                matchDatasets(dataset.getId()).toPredicate(root, query, builder),
                matcheEquals(getTimespanStart(), DataEntity.PROPERTY_SAMPLING_TIME_START).toPredicate(root, query,
                        builder));
        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    public Optional<DataEntity> matchEnd(DatasetEntity dataset, EntityManager entityManager) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DataEntity> query = builder.createQuery(DataEntity.class);
        Root<DataEntity> root = query.from(DataEntity.class);
        query.select(root).orderBy(builder.desc(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END))).where(
                matchDatasets(dataset.getId()).toPredicate(root, query, builder),
                matcheEquals(getTimespanEnd(), DataEntity.PROPERTY_SAMPLING_TIME_END).toPredicate(root, query,
                        builder));
        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    public Optional<DataEntity> matchClosestBeforeStart(DatasetEntity dataset, EntityManager entityManager) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DataEntity> query = builder.createQuery(DataEntity.class);
        Root<DataEntity> root = query.from(DataEntity.class);
        query.select(root).orderBy(builder.desc(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END))).where(
                matchDatasets(dataset.getId()).toPredicate(root, query, builder),
                matchBefore(getTimespanStart()).toPredicate(root, query, builder));
        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    public Optional<DataEntity> matchClosestAfterEnd(DatasetEntity dataset, EntityManager entityManager) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DataEntity> query = builder.createQuery(DataEntity.class);
        Root<DataEntity> root = query.from(DataEntity.class);
        query.select(root).orderBy(builder.asc(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END))).where(
                matchDatasets(dataset.getId()).toPredicate(root, query, builder),
                matchAfter(getTimespanEnd()).toPredicate(root, query, builder));

        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    public Long count(DatasetEntity dataset, EntityManager entityManager) {
        // Criteria c =
        // getDefaultCriteria().add(Restrictions.eq(DataEntity.PROPERTY_DATASET_ID, dataset.getId()))
        // .setProjection(Projections.rowCount());
        // return (Long) c.uniqueResult();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<DataEntity> root = query.from(DataEntity.class);
        query.select(builder.count(root));
        query.where(matchDatasets(dataset.getId()).toPredicate(root, query, builder));
        return entityManager.createQuery(query).getSingleResult();
    }

    public DataEntity min(DatasetEntity dataset, EntityManager entityManager) {
        Class<?> clazz = getClass(dataset);
        if (clazz != null) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<DataEntity> query = (CriteriaQuery<DataEntity>) builder.createQuery(clazz);

            Subquery sq = createSubQuery(query, dataset);
            Root<DataEntity> min = sq.from(clazz);
            sq.select(builder.min(min.get(DataEntity.PROPERTY_VALUE))).distinct(true)
                    .where(matchDatasets(dataset.getId()).toPredicate(min, query, builder));

            Root<DataEntity> root = (Root<DataEntity>) query.from(clazz);
            query.select(root).orderBy(new OrderImpl(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END), false));
            query.where(builder.and(builder.equal(root.get(DataEntity.PROPERTY_VALUE), sq.getSelection()),
                    matchDatasets(dataset.getId()).toPredicate(root, query, builder)));
            return entityManager.createQuery(query).setMaxResults(1).getSingleResult();
        }
        return null;
    }

    public DataEntity max(DatasetEntity dataset, EntityManager entityManager) {
        Class<?> clazz = getClass(dataset);
        if (clazz != null) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<DataEntity> query = (CriteriaQuery<DataEntity>) builder.createQuery(clazz);

            Subquery sq = createSubQuery(query, dataset);
            Root<DataEntity> max = sq.from(clazz);
            sq.select(builder.max(max.get(DataEntity.PROPERTY_VALUE))).distinct(true)
                    .where(matchDatasets(dataset.getId()).toPredicate(max, query, builder));

            Root<DataEntity> root = (Root<DataEntity>) query.from(clazz);
            query.select(root).orderBy(new OrderImpl(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END), false));
            query.where(builder.and(builder.equal(root.get(DataEntity.PROPERTY_VALUE), sq.getSelection()),
                    matchDatasets(dataset.getId()).toPredicate(root, query, builder)));
            return entityManager.createQuery(query).setMaxResults(1).getSingleResult();
        }
        return null;
    }

    public BigDecimal average(DatasetEntity dataset, EntityManager entityManager) {
        Class<?> clazz = getClass(dataset);
        if (clazz != null) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Double> query = builder.createQuery(Double.class);
            Root<DataEntity> root = (Root<DataEntity>) query.from(clazz);
            query.select(builder.avg(root.get(DataEntity.PROPERTY_VALUE)));
            query.where(matchDatasets(dataset.getId()).toPredicate(root, query, builder));
            return BigDecimal.valueOf(entityManager.createQuery(query).getSingleResult());
        }
        return null;
    }

    private Subquery<?> createSubQuery(CriteriaQuery<?> query, DatasetEntity dataset) {
        switch (dataset.getValueType()) {
            case count:
                return query.subquery(Integer.class);
            case quantity:
                return query.subquery(BigDecimal.class);
            default:
                return null;
        }
    }

    private Class<?> getClass(DatasetEntity dataset) {
        switch (dataset.getValueType()) {
            case count:
                return CountDataEntity.class;
            case quantity:
                return QuantityDataEntity.class;
            default:
                return null;
        }
    }

    private Specification<DataEntity> matcheEquals(Date date, String property) {
        return (root, query, builder) -> {
            return builder.equal(root.get(property), date);
        };
    }

    private Specification<DataEntity> matchBefore(Date date) {
        return (root, query, builder) -> {
            return builder.lessThan(root.get(DataEntity.PROPERTY_SAMPLING_TIME_START), date);
        };
    }

    private Specification<DataEntity> matchAfter(Date date) {
        return (root, query, builder) -> {
            return builder.greaterThan(root.get(DataEntity.PROPERTY_SAMPLING_TIME_END), date);
        };
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
    public Specification<DataEntity> matchesSpatially() {
        final Geometry geometry = dbQuery.getSpatialFilter();
        if ((geometry == null) || geometry.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            return new IntersectsPredicate((CriteriaBuilderImpl) builder,
                    root.get(DataEntity.PROPERTY_GEOMETRY_ENTITY).get(DataEntity.PROPERTY_GEOMETRY),
                    new LiteralExpression<>((CriteriaBuilderImpl) builder, geometry), entityManager);
        };
    }

}
