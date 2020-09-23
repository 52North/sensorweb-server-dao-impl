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

package org.n52.series.db.old.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.joda.time.DateTime;
import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.QueryUtils;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @param <T>
 *        the data entity type
 */
@Transactional
@SuppressWarnings("rawtypes")
public class DataDao<T extends DataEntity> extends AbstractDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDao.class);

    private static final Order DEFAULT_ORDER = Order.asc(DataEntity.PROPERTY_SAMPLING_TIME_END);

    private final Class<T> entityType;

    @SuppressWarnings("unchecked")
    public DataDao(Session session) {
        this(session, (Class<T>) DataEntity.class);
    }

    public DataDao(Session session, Class<T> clazz) {
        super(session);
        this.entityType = clazz;
    }

    @Override
    public T getInstance(Long key, DbQuery parameters) {
        LOGGER.debug("get instance '{}': {}", key, parameters);
        return entityType.cast(session.get(entityType, key));
    }

    /**
     * Retrieves all available observation instances.
     *
     * @param q query parameters.
     *
     * @return all instances matching the given query parameters.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery q) {
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        query.addTimespanTo(criteria);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(toSQLString(criteria));
        }
        return criteria.list();
    }

    /**
     * Retrieves all available observation instances belonging to a particular series.
     *
     * @param dataset
     *        the dataset the observations belongs to.
     * @param q
     *        some query parameters to restrict result.
     * @return all observation entities belonging to the given series which match the given query.
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllInstancesFor(Long dataset, DbQuery q) {
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("get all instances for series '{}': {}", dataset, query);
        Criteria criteria = getDefaultCriteria(query);
        criteria.createCriteria(DataEntity.PROPERTY_DATASET).add(Restrictions.eq(DatasetEntity.PROPERTY_ID, dataset));
        query.addTimespanTo(criteria);
        return criteria.list();
    }

    public List<DataEntity<?>> getAllInstancesFor(Set<Long> series, DbQuery query) {
        Criteria criteria = getDefaultCriteria(query)
                .add(Restrictions.in(DataEntity.PROPERTY_DATASET_ID, series))
                .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE));
        query.addTimespanTo(criteria);
        return criteria.list();
    }

    @Override
    protected Class<T> getEntityClass() {
        return entityType;
    }

    @Override
    protected String getDatasetProperty() {
        // there's no series property for observation
        return "";
    }

    @Override
    public Criteria getDefaultCriteria(final DbQuery query) {
        return getDefaultCriteria(query, DEFAULT_ORDER);
    }

    private Criteria getDefaultCriteria() {
        Criteria criteria =
                session.createCriteria(entityType).add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE));
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

    private Criteria getDefaultCriteria(Order order) {
        return getDefaultCriteria().addOrder(order);
    }

    private Criteria getDefaultCriteria(final DbQuery query, Order order) {
        Criteria criteria = getDefaultCriteria(order);

        query.addSpatialFilter(criteria);
        query.addResultTimeFilter(criteria);
        query.addOdataFilterForData(criteria);

//        criteria = query.isComplexParent()
//                ? criteria.add(Restrictions.isNull(DataEntity.PROPERTY_PARENT))
//                : criteria.add(Restrictions.isNotNull(DataEntity.PROPERTY_PARENT));

        criteria.add(Restrictions.isNull(DataEntity.PROPERTY_PARENT));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    public T getClosestOuterPreviousValue(final DatasetEntity dataset, final DateTime lowerBound, final DbQuery query) {
        final String column = DataEntity.PROPERTY_SAMPLING_TIME_START;
        final Order order = Order.desc(DataEntity.PROPERTY_SAMPLING_TIME_START);
        final Criteria criteria = createDataCriteria(column, dataset, query, order);
        return (T) criteria.add(Restrictions.lt(column, lowerBound.toDate()))
                           .setMaxResults(1)
                           .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T getClosestOuterNextValue(final DatasetEntity dataset, final DateTime upperBound, final DbQuery query) {
        final String column = DataEntity.PROPERTY_SAMPLING_TIME_END;
        final Order order = Order.asc(DataEntity.PROPERTY_SAMPLING_TIME_END);
        final Criteria criteria = createDataCriteria(column, dataset, query, order);
        return (T) criteria.add(Restrictions.gt(column, upperBound.toDate()))
                           .setMaxResults(1)
                           .uniqueResult();
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public T getDataValueViaTimeend(DatasetEntity series, DbQuery query) {
        Date timeend = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(timeend, DataEntity.PROPERTY_SAMPLING_TIME_END, series, query);
        return (T) criteria.uniqueResult();
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public T getDataValueViaTimestart(DatasetEntity series, DbQuery query) {
        Date timestart = series.getFirstValueAt();
        Criteria criteria = createDataAtCriteria(timestart, DataEntity.PROPERTY_SAMPLING_TIME_START, series, query);
        return (T) criteria.uniqueResult();
    }

    @Deprecated
    public GeometryEntity getValueGeometryViaTimeend(DatasetEntity series, DbQuery query) {
        Date lastValueAt = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(lastValueAt, DataEntity.PROPERTY_SAMPLING_TIME_END, series, query);
        criteria.setProjection(Projections.property(DataEntity.PROPERTY_GEOMETRY_ENTITY));
        return (GeometryEntity) criteria.uniqueResult();
    }

    private Criteria createDataAtCriteria(final Date timestamp,
                                          final String column,
                                          final DatasetEntity dataset,
                                          final DbQuery query) {
        LOGGER.debug("get data @{} for '{}'", new DateTime(timestamp.getTime()), dataset.getId());
        return createDataCriteria(column, dataset, query).add(Restrictions.eq(column, timestamp));
    }

    private Criteria createDataCriteria(String column, DatasetEntity dataset, DbQuery query) {
        return createDataCriteria(column, dataset, query, DEFAULT_ORDER);
    }

    private Criteria createDataCriteria(String column, DatasetEntity dataset, DbQuery query, Order order) {
        Criteria criteria = getDefaultCriteria(query, order);
        criteria.add(Restrictions.eq(DataEntity.PROPERTY_DATASET, dataset));

        IoParameters parameters = query.getParameters();
        if (parameters.isAllResultTimes()) {
            // no filter needed
            return criteria;
        } else if (!parameters.getResultTimes()
                              .isEmpty()) {
            // filter based on given result times
            return query.addResultTimeFilter(criteria);
        } else {
            // values for oldest result time
            String rtAlias = "rtAlias";
            String rtColumn = QueryUtils.createAssociation(rtAlias, column);
            String rtDatasetId = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_DATASET);
            String rtResultTime = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_RESULT_TIME);
            DetachedCriteria maxResultTimeQuery = DetachedCriteria.forClass(getEntityClass(), rtAlias);
            maxResultTimeQuery.add(Restrictions.eq(DataEntity.PROPERTY_DATASET, dataset))
                              .setProjection(Projections.projectionList()
                                                        .add(Projections.groupProperty(rtColumn))
                                                        .add(Projections.groupProperty(rtDatasetId))
                                                        .add(Projections.max(rtResultTime)));
            criteria.add(Subqueries.propertiesIn(new String[] {
                column,
                DataEntity.PROPERTY_DATASET,
                DataEntity.PROPERTY_RESULT_TIME
            }, maxResultTimeQuery));

        }
        return criteria;
    }

    @SuppressWarnings("unchecked")
    public T getLastObservationForSampling(DatasetEntity dataset, Date date, DbQuery query) {
        final String column = DataEntity.PROPERTY_SAMPLING_TIME_END;
        final Order order = Order.desc(column);
        final Criteria criteria = createDataCriteria(column, dataset, query, order);
        return (T) criteria.add(Restrictions.lt(column, DateUtils.addMilliseconds(date, 1)))
                           .setMaxResults(1)
                           .uniqueResult();
    }


    @SuppressWarnings("unchecked")
    public T getMax(DatasetEntity dataset) {
        Criteria c = getDefaultCriteria(Order.desc(DataEntity.PROPERTY_SAMPLING_TIME_END));
        addDatasetRestriction(c, dataset);
        DetachedCriteria filter = DetachedCriteria.forClass(getEntityClass());
        filter.setProjection(Projections.max(DataEntity.PROPERTY_VALUE));
        filter.add(Restrictions.eq(DataEntity.PROPERTY_DATASET_ID, dataset.getId()));
        c.add(Subqueries.propertyEq(DataEntity.PROPERTY_VALUE, filter));
        return (T) c.setMaxResults(1).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T getMin(DatasetEntity dataset) {
        Criteria c = getDefaultCriteria(Order.desc(DataEntity.PROPERTY_SAMPLING_TIME_END));
        addDatasetRestriction(c, dataset);
        DetachedCriteria filter = DetachedCriteria.forClass(getEntityClass());
        filter.setProjection(Projections.min(DataEntity.PROPERTY_VALUE));
        filter.add(Restrictions.eq(DataEntity.PROPERTY_DATASET_ID, dataset.getId()));
        c.add(Subqueries.propertyEq(DataEntity.PROPERTY_VALUE, filter));
        return (T) c.setMaxResults(1).uniqueResult();
    }

    public Long getCount(DatasetEntity dataset) {
        Criteria c =
                getDefaultCriteria().add(Restrictions.eq(DataEntity.PROPERTY_DATASET_ID, dataset.getId()))
                        .setProjection(Projections.rowCount());
        return (Long) c.uniqueResult();
    }

    public BigDecimal getAvg(DatasetEntity dataset) {
        Criteria c = getDefaultCriteria();
        addDatasetRestriction(c, dataset);
        c.setProjection(Projections.avg(DataEntity.PROPERTY_VALUE));
        return BigDecimal.valueOf((Double) c.uniqueResult());
    }

    private void addDatasetRestriction(Criteria c, DatasetEntity dataset) {
        c.add(Restrictions.eq(DataEntity.PROPERTY_DATASET_ID, dataset.getId()));
    }

}
